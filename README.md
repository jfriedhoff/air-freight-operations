# Air Freight
 
Vlucht- en vrachtbeheer voor een luchtvrachtmaatschappij. Project voor Software Architecture 2026–27.

## Team

* Romeo Weyns & Jonas Friedhoff
* romeo-jonas

* Coach: Lars Willemsens
* Academiejaar: 26-27
* Klasgroep: IAI3
 
## Documentatie
 
De analyse staat in [`architecture/analyse.md`](architecture/analyse.md).

## Opstarten

Nodig: JDK 25, Docker en Node.js met npm.

```bash
docker compose up -d      # PostgreSQL 5443, RabbitMQ 5672/15672, Keycloak 8180, MySQL 3307
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

Frontend tijdens het ontwikkelen: `npm run dev` in `flight-operations/flight-operations-frontend` of `load-planning/load-planning-frontend`. Vite draait op 5173 en stuurt `/api` door naar de service.

Elke context heeft een eigen databank (`flight_operations`, `load_planning`, `messaging`) op dezelfde PostgreSQL. Het script `init-scripts/sql/init.sql` maakt ze aan bij de eerste start. Bestond de map `postgresql/` al, verwijder ze dan en start opnieuw.

De integratietests van Flight Operations gebruiken een eigen databank, `flight_operations_test`, en versturen niets naar RabbitMQ. Bestond de map `postgresql/` al, maak die databank dan één keer aan:

```bash
docker compose exec app_postgres psql -U user -d postgres -c "CREATE DATABASE flight_operations_test OWNER flight_operations"
```

RabbitMQ: http://localhost:15672 (`user` / `password`). Keycloak: http://localhost:8180 (`admin` / `admin`).

### Aanmelden

Keycloak importeert bij de eerste start de realm `air-freight` uit `keycloak/air-freight-realm.json`: de rollen `flight-planner` en `load-planner`, de clients van de frontends en de services, en twee testgebruikers.

| Gebruiker | Wachtwoord | Rol |
| --- | --- | --- |
| `flight_planner` | `password` | flight planner |
| `load_planner` | `password` | load planner |

Draaide Keycloak al van vóór deze wijziging, dan bestaat de realm nog niet: `docker compose up -d --force-recreate idp_keycloak`. Wijzigt het realmbestand later, verwijder dan de realm `air-freight` in de Keycloak-console en herstart Keycloak; een bestaande realm wordt niet opnieuw geïmporteerd.

De API antwoordt 401 zonder token en 403 met de verkeerde rol. `flight-operations/flight-operations-service/http/flight_api.http` meldt zich aan als `flight_planner` en probeert de API uit. De clientgeheimen in het realmbestand dienen alleen voor die lokale tests.

## Berichten tussen de services

De drie services praten alleen via RabbitMQ met elkaar. Elke service publiceert op haar eigen exchange en leest van één eigen queue.

| Service | Publiceert op | Leest van |
| --- | --- | --- |
| Flight Operations | `flight-operations.events` | `flight-operations.messaging-events` |
| Load Planning | `load-planning.events` | `load-planning.messaging-events` |
| Messaging | `messaging.events` (voor de andere services), `messaging.partner-messages` (FLM, MVT, CPM) | `messaging.schedule`, `messaging.bookings`, `messaging.flights`, `messaging.loads` |

- De partnerberichten komen in de queue `partners.all`, zolang er geen partner aangesloten is.
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
