# Architecture overview: where the code lives

The system has three bounded contexts, each a Spring Boot service with its own database, plus a simulator that
stands in for the existing source systems. This page is the map for a developer who has to change or extend the
code. The domain itself (the rules, the ubiquitous language, the context map) is in
[`architecture/analyse.md`](../architecture/analyse.md), in Dutch.

## In short

- Four Gradle modules with a service each: `flight-operations`, `load-planning`, `messaging`, `simulator`. The
  first two also have a frontend module that is built into their service.
- Every service has the same four layers: `api` → `application` → `domain` ← `infrastructure`. The domain has no
  Spring or JPA; it holds the rules and the interfaces of what it needs (repositories, catalogs, ports).
- `DddArchTest` in every service fails the build when a dependency points the wrong way or a DDD rule is broken.
- The services share no code. They only talk through events on RabbitMQ, and only through Messaging: Flight
  Operations and Load Planning never send each other anything directly.
- Copies are deliberate: the outbox, `FlightNumber`, `AirportCode`, `SetAsideRecoverer` and the test helpers exist
  once per service, and ten frontend files exist in both frontends.

## The services

| Service | Port | Owns | Talks to |
| --- | --- | --- | --- |
| Flight Operations | 8080 | `Flight` with its delays, ETD and ETA, ATD and ATA, `FlightPlanner` | Messaging (flight events out; new flights and the CPM in) |
| Load Planning | 8081 | `FlightCargo` with its ULDs, load positions, load checks, finalizing, `LoadPlanner`, a read-only copy of the flight (`FlightInfo`) | Messaging (load finalized out; flight events and ULD bookings in) |
| Messaging | none | `ReceivedMessage` (the inbox) and `OutgoingMessage` (FLM, MVT and CPM for the partners) | the source systems, the partners and the other two services |
| Simulator | 8090, not in production | nothing: no domain | sends SSM and ULD messages as the source systems would |

Messaging has no REST API, so it starts no web server; it only uses RabbitMQ and its database.

## The layers of one service

The package of a service is `be.kdg.airfreight.<context>`, for example `be.kdg.airfreight.loadplanning`.

| Package | What goes in it | Examples (Load Planning) |
| --- | --- | --- |
| `api` | REST controllers, request and response DTOs, `ErrorHandling` | `api/flightcargo/FlightCargoController`, `UldDto` |
| `application` | One application service per group of use cases, the commands they take (`application/command`) and the queries (`application/query`). A method loads the aggregate, calls it, saves it and publishes its events, in one transaction | `LoadService.assign`, `AssignLoadPositionCommand`, `FlightsToLoadQuery` |
| `domain` | Aggregates, entities, value objects, domain events, domain services, and the interfaces of repositories and catalogs. Annotated with jMolecules (`@AggregateRoot`, `@Entity`, `@ValueObject`, `@Identity`); every package is `@NullMarked` (JSpecify) | `FlightCargo`, `Uld`, `LoadPosition`, `LoadCheck`, `FlightCargoRepository` |
| `infrastructure` | Everything that implements a domain interface or touches a technology: JPA, RabbitMQ, Keycloak, configuration | `flightcargo/DbFlightCargoRepository`, `messaging/OutboxRelay`, `security/SecurityConfig`, `config/PolicyConfig` |

The simulator has no domain: its port `StreamPublisher` is in `application`, and `infrastructure` implements it.

## What `DddArchTest` enforces

`DddArchTest` (ArchUnit) runs with the other tests of a service:

- **The jMolecules DDD rules** (`JMoleculesDddRules.all()`): aggregates refer to each other only by id, every entity
  has an `@Identity`, value objects do not refer to entities.
- **The layers point to the domain.** Nothing calls `api`. `application` is only called by `api` and by the
  *inbound adapters*. The rest of `infrastructure` is only used by the inbound adapters.
- **Inbound adapters** are the classes in `infrastructure` with a `@RabbitListener` or `@Scheduled` method: the
  listeners and the timers (outbox relays, retention). They are the only infrastructure that may call a use case.
  They stay in `infrastructure/messaging`, as in the course slides.
- **The domain does not use the framework:** no dependency on `org.springframework..` or `jakarta..`.

The simulator has `LayerArchTest` instead: `api` → `application` ← `infrastructure`.

## Where a change goes

| You want to | Start in |
| --- | --- |
| Change a rule (a delay, the loading period, a load check) | The aggregate or the domain service in `domain`; its `@unit` scenario in the `.feature` file of the story |
| Add a use case | A command in `application/command`, a method on the application service, and a controller method or a listener case |
| Add an endpoint | `api`, with a DTO; the domain stays the same if the use case exists |
| Add or change an event between services | [`messaging.md`](messaging.md), "Adding an event" |
| Change what is stored | [`persistence.md`](persistence.md) |
| Change an error answer or a log line | [`errors-and-logging.md`](errors-and-logging.md) |
| Change a policy value (5 min, 12 h / 20 min, 10 %, 7 days) | Configuration only: [`policy-values.md`](policy-values.md) |
| Add an MVT type, a load check, an aircraft type or a message format | `analyse.md` §10 lists where each one goes |
| Change who may do what | [`security-and-tokens.md`](security-and-tokens.md) |

## The frontends

Each frontend is a set of TypeScript modules built with Vite into `src/main/resources/static/` of its own service
(generated, not in Git). A page only calls the API of its own service, with a token for that service; a link brings
the planner to the other context, and Keycloak signs them in there without a password.

- `src/ts/domain/*.ts`: what a page decides (which actions a role and a state allow, Brussels time, the Gantt bars,
  the deck). Pure functions with tests in Node: [`frontend-tests.md`](frontend-tests.md).
- `src/ts/presenter/*.ts`: putting data on the page and reacting to clicks.
- `src/ts/infrastructure/*.ts`: the REST calls and Keycloak.

The server decides every rule again; a page only hides what the server would refuse.

## Tests

| Kind | Where | Runs |
| --- | --- | --- |
| `@unit` scenarios and plain JUnit on the domain | `src/test/resources/features`, `UnitAcceptanceTest`, `src/test/java/.../domain` | every build, no Docker |
| `@integration` scenarios and MockMvc tests | `IntegrationAcceptanceTest`, `api/*Test` | every build; need PostgreSQL and RabbitMQ from Docker Compose |
| Contract tests | `contracts/`, `ContractTest` per service | every build, no Docker |
| `@e2e` and `@ui` scenarios | by hand | [`e2e-hand-checks.md`](e2e-hand-checks.md) |
| Mutation tests (PIT) | `./gradlew :<module>:pitest` | by hand, and a manual job in the pipeline |

A `./gradlew :<module>:test` always runs the `@integration` scenarios too, so Docker Compose must be up. Do not run
the tests while a service of the same context is running locally: both read the same queues.

## References

- [`architecture/analyse.md`](../architecture/analyse.md): the domain, the context map (§5), the deployment (§6), the
  tactical design (§7) and the design for change (§10).
- [`cucumber-acceptance-tests.md`](cucumber-acceptance-tests.md): how the scenarios are written and run.
- [ArchUnit — User Guide](https://www.archunit.org/userguide/html/000_Index.html): layered architecture rules.
- [jMolecules](https://github.com/xmolecules/jmolecules): the DDD annotations and their ArchUnit rules.
