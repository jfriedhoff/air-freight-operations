# Air Freight
 
Vlucht- en vrachtbeheer voor een luchtvrachtmaatschappij. Project voor Software Architecture 2026–27.

## Team

* Romeo Weyns & Jonas Friedhoff
* romeo-jonas

* Coach: Lars Willemsens
* Academiejaar: 26-27
* Klasgroep: IAI3
 
## Documentatie
 
- De analyse: [`architecture/analyse.md`](architecture/analyse.md). Onze vragen aan de coach en zijn antwoorden staan in §8 en in de reflectie.
- De reflectie per sprint: [`REFLECTIE.md`](REFLECTIE.md).
- Uitleg bij de techniek, in het Engels, in [`docs/`](docs/):
  - [`architecture-overview.md`](docs/architecture-overview.md): waar de code staat, de lagen en wat `DddArchTest` afdwingt
  - [`messaging.md`](docs/messaging.md): outbox, inbox, volgorde, dead letter queues, een event toevoegen en een bericht herstellen
  - [`errors-and-logging.md`](docs/errors-and-logging.md): de foutantwoorden van de API en de logniveaus
  - [`persistence.md`](docs/persistence.md): domein en JPA, optimistic locking, transacties en een schemawijziging
  - [`ci-pipeline.md`](docs/ci-pipeline.md): de testen in GitLab
  - [`cucumber-acceptance-tests.md`](docs/cucumber-acceptance-tests.md): de acceptatiecriteria als Cucumber-scenario's
  - [`contract-tests.md`](docs/contract-tests.md): de voorbeeldberichten tussen de services
  - [`e2e-hand-checks.md`](docs/e2e-hand-checks.md): de `@e2e`- en `@ui`-scenario's met de hand, met hun resultaten
  - [`frontend-tests.md`](docs/frontend-tests.md): de testen van de logica van de pagina's
  - [`mutation-testing-pit.md`](docs/mutation-testing-pit.md): mutatietesten met PIT
  - [`policy-values.md`](docs/policy-values.md): de beleidswaarden aanpassen zonder nieuwe release
  - [`security-and-tokens.md`](docs/security-and-tokens.md): Keycloak, rollen en tokens

## Opstarten

Nodig: JDK 25, Docker en Node.js met npm.

```bash
docker compose up -d      # PostgreSQL 5443, RabbitMQ 5672/15672, Keycloak 8180, MySQL 3307, partnerservice 8000
./gradlew build           # compileert en test alle modules, bouwt ook de frontends
```

De tests starten JPA en hebben dus de databank nodig: eerst `docker compose up -d`.

| Module | Start | Poort |
| --- | --- | --- |
| Flight Operations | `./gradlew :flight-operations:flight-operations-service:bootRun` | 8080 |
| Load Planning | `./gradlew :load-planning:load-planning-service:bootRun` | 8081 |
| Messaging | `./gradlew :messaging:messaging-service:bootRun` | 8082 |
| Simulator (niet in productie) | `./gradlew :simulator:simulator-service:bootRun` | 8090 |

Eén module testen: `./gradlew :flight-operations:flight-operations-service:test`.
Eén test: voeg `--tests "be.kdg.airfreight.flightoperations.DddArchTest"` toe.

Elke merge request en elke push naar `main` draait alle tests in een GitLab-pipeline, per service met een eigen PostgreSQL en RabbitMQ. De line coverage staat bij de jobs. Uitleg: [`docs/ci-pipeline.md`](docs/ci-pipeline.md).

Frontend tijdens het ontwikkelen: `npm run dev` in `flight-operations/flight-operations-frontend` of `load-planning/load-planning-frontend`. Vite draait op 5173 en stuurt `/api` door naar de service.

Elke context heeft een eigen databank (`flight_operations`, `load_planning`, `messaging`) op dezelfde PostgreSQL. Het script `init-scripts/sql/init.sql` maakt ze aan bij de eerste start. Bestond de map `postgresql/` al, verwijder ze dan en start opnieuw.

De integratietests van elke service gebruiken een eigen databank: `flight_operations_test`, `load_planning_test` en `messaging_test`. Zo raken ze de gegevens van een draaiende service nooit. Bestond de map `postgresql/` al, maak die databanken dan één keer aan:

```bash
docker compose exec app_postgres psql -U user -d postgres -c "CREATE DATABASE flight_operations_test OWNER flight_operations"
docker compose exec app_postgres psql -U user -d postgres -c "CREATE DATABASE load_planning_test OWNER load_planning"
docker compose exec app_postgres psql -U user -d postgres -c "CREATE DATABASE messaging_test OWNER messaging"
```

RabbitMQ: http://localhost:15672 (`user` / `password`). Keycloak: http://localhost:8180 (`admin` / `admin`).

### Aanmelden

Keycloak importeert bij de eerste start de realm `air-freight` uit `keycloak/air-freight-realm.json`: de rollen `flight-planner` en `load-planner`, de clients van de frontends en de services, en twee testgebruikers.

| Gebruiker | Wachtwoord | Rol |
| --- | --- | --- |
| `flight_planner` | `FLIGHT_PLANNER_PASSWORD` uit `.env` | flight planner |
| `load_planner` | `LOAD_PLANNER_PASSWORD` uit `.env` | load planner |

De geheimen van de realm staan niet in Git. Het realmbestand bevat alleen plaatshouders (`${LOAD_PLANNER_PASSWORD}`, …); Docker Compose geeft de waarden uit het bestand `.env` door aan Keycloak, dat ze bij het importeren invult. Maak `.env` één keer aan vóór de eerste `docker compose up`:

```bash
cp .env.example .env    # en kies zelf een waarde voor elke regel
```

Zonder `.env` weigert Docker Compose te starten en zegt het welke waarde ontbreekt.

Draaide Keycloak al van vóór deze wijziging, dan bestaat de realm nog niet: `docker compose up -d --force-recreate idp_keycloak`. Wijzigt het realmbestand later, verwijder dan de realm `air-freight` in de Keycloak-console en herstart Keycloak; een bestaande realm wordt niet opnieuw geïmporteerd.

De API's antwoorden 401 zonder token en 403 met de verkeerde rol. Twee `.http`-bestanden melden zich aan en proberen een API uit: `flight-operations/flight-operations-service/http/flight_api.http` (als `flight_planner`) en `load-planning/load-planning-service/http/cargo_api.http` (als `load_planner` en als `flight_planner`).

Het clientgeheim en de wachtwoorden staan niet in Git. Maak naast elk `http-client.env.json` één keer een bestand `http-client.private.env.json` aan (het staat in `.gitignore`), met de waarden uit je `.env`:

```json
{
  "dev": {
    "client-secret": "FLIGHT_OPERATIONS_SERVICE_CLIENT_SECRET",
    "password": "FLIGHT_PLANNER_PASSWORD"
  }
}
```

Voor Load Planning, dat zich als beide planners aanmeldt en ook toont dat een token voor een andere service geweigerd wordt (de audience):

```json
{
  "dev": {
    "client-secret": "LOAD_PLANNING_SERVICE_CLIENT_SECRET",
    "flight-operations-client-secret": "FLIGHT_OPERATIONS_SERVICE_CLIENT_SECRET",
    "load-planner-password": "LOAD_PLANNER_PASSWORD",
    "flight-planner-password": "FLIGHT_PLANNER_PASSWORD"
  }
}
```

### Pagina's

| Context | Pagina | Adres na `bootRun` |
| --- | --- | --- |
| Flight Operations | Startpagina: de ingang na het aanmelden, met het overzicht van de vluchten van vandaag (UTC) | http://localhost:8080/ |
| Flight Operations | Vluchtdetails | http://localhost:8080/html/flight-details.html?flightId=… |
| Flight Operations | Gantt chart: de vluchten van één dag op een tijdlijn | http://localhost:8080/html/gantt-chart.html |
| Load Planning | Startpagina | http://localhost:8081/ |
| Load Planning | Te beladen vluchten | http://localhost:8081/html/flights-to-load.html |
| Load Planning | Cargo-pagina | via een vlucht in de lijst van te beladen vluchten |

Na het aanmelden op de ene context opent de andere zonder opnieuw een wachtwoord te vragen: beide frontends gebruiken dezelfde realm. Het adres van de andere context komt uit een Vite-variabele, `VITE_LOAD_PLANNING_URL` (standaard `http://localhost:8081`) en `VITE_FLIGHT_OPERATIONS_URL` (standaard `http://localhost:8080`). Draai maar één frontend tegelijk met `npm run dev`: beide gebruiken poort 5173, en een andere poort is geen geldige redirect in de realm. `npm run dev -- --strictPort` faalt dan duidelijk in plaats van uit te wijken naar 5174.

Keycloak bepaalt wie planner is. Een nieuwe collega met de rol `flight-planner` of `load-planner` kan meteen werken, zonder herstart: de service registreert een planner bij zijn eerste wijziging, met de id van het Keycloak-account en de naam uit het token (`name`, anders `preferred_username`).

### Bekende kwetsbaarheden in dependencies

Elke service past de OWASP dependency-check plugin toe. Die vergelijkt de dependencies met de National Vulnerability Database (NVD) en faalt bij een bekende kwetsbaarheid met een CVSS-score van 7 of meer:

```bash
export NVD_API_KEY=<je sleutel>     # https://nvd.nist.gov/developers/request-an-api-key
./gradlew :load-planning:load-planning-service:dependencyCheckAnalyze   # rapport in build/reports van de service
```

Op Windows zet je de sleutel zo:

```powershell
$env:NVD_API_KEY = "<je sleutel>"    # PowerShell, alleen dit venster
set NVD_API_KEY=<je sleutel>         # cmd, alleen dit venster
setx NVD_API_KEY "<je sleutel>"      # blijvend, geldt pas in nieuwe vensters (ook IntelliJ herstarten)
gradlew.bat :load-planning:load-planning-service:dependencyCheckAnalyze
```

De eerste run haalt de hele databank op; zonder sleutel duurt dat uren. De controle hoort niet bij `./gradlew build`, zodat de build snel blijft en zonder netwerk werkt.

Twee meldingen gaan over een ander product met een gelijkaardige naam en staan in `dependency-check-suppressions.xml`, elk voor één CVE op één library, met de reden erbij. Elke andere melding op die libraries laat de controle nog falen.

## Berichten tussen de services

De drie services praten alleen via RabbitMQ met elkaar. Elke service publiceert op haar eigen exchange en leest van één eigen queue.

| Service | Publiceert op | Leest van |
| --- | --- | --- |
| Flight Operations | `flight-operations.events` | `flight-operations.messaging-events` |
| Load Planning | `load-planning.events` | `load-planning.messaging-events` |
| Messaging | `messaging.events` (voor de andere services), `partner-exchange` (FLM, MVT, CPM) | `messaging.schedule`, `messaging.bookings`, `messaging.flights`, `messaging.loads` |

- De partnerberichten gaan naar `partner-exchange`. De partnerservice van de cursus (`docker-compose.override.yml`, start mee met `docker compose up -d`) leest ze via haar eigen queue `partner-inbox` en toont ze op http://localhost:8000. Messaging zet ze ook in de queue `partners.all`, om ze in RabbitMQ te kunnen bekijken.
- Een bericht dat een service niet kan verwerken, belandt in haar dead letter queue: `flight-operations.dlq`, `load-planning.dlq` of `messaging.dlq`.
- Elke service bewaart haar uitgaande events eerst in de tabel `outbox_events` en verstuurt ze daarna. Is RabbitMQ onbereikbaar, dan volgen ze zodra het terug is.
- Start nooit twee instanties van dezelfde service tegelijk: de volgorde van de berichten hangt af van één lezer per queue.
- `messaging/messaging-service/http/messaging.http` stuurt berichten rechtstreeks naar een exchange, om de Messaging-service apart uit te proberen.
- De simulator bootst de twee bronsystemen na: `POST /api/flight-schedules` stuurt één SSM per record naar `network-planning.messages`, `POST /api/cargo-bookings` één ULD-bericht per record naar `cargo-booking.messages`. Voorbeelden staan in `simulator/simulator-service/http/simulator.http`. Start Messaging eerst: die maakt de queues aan. Met `--spring.profiles.active=production` bestaan de endpoints niet (404).

De databanken worden niet meer leeggemaakt bij elke start (`spring.jpa.hibernate.ddl-auto=update`). Wie al een lokale omgeving had van vóór deze wijziging, doet één keer:

```bash
docker compose exec rabbitmq rabbitmqctl delete_queue messaging.flights
docker compose exec postgres psql -U load_planning -d load_planning -c 'DROP TABLE IF EXISTS ulds, flight_cargos'
```

Verandert een mapping later op een manier die `update` niet kan volgen (een kolom die verdwijnt of van type verandert), verwijder dan de tabel of de map `postgresql/` en start opnieuw.
