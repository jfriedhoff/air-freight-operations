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

RabbitMQ: http://localhost:15672 (`user` / `password`). Keycloak: http://localhost:8180 (`admin` / `admin`).
