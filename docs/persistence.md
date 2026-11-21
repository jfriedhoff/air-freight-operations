# Persistence: how an aggregate is stored

The domain knows nothing of JPA. Every aggregate has a separate `Jpa*` entity in `infrastructure`, and a repository
there implements the interface of the domain. This page explains that mapping, the locking that stops two planners
from overwriting each other, the transactions, and what to do when the schema changes.

## In short

- Domain class ↔ `Jpa*` entity, with `JpaX.fromDomain(x)` and `jpaX.toDomain()`. `toDomain` rebuilds the aggregate
  with its static `hydrate(...)`, which sets the stored state without running the rules again.
- `DbXRepository` (in `infrastructure/<aggregate>`) implements the domain's `XRepository` and delegates to a Spring
  Data `JpaXRepository` (in `infrastructure/<aggregate>/jpa`). One repository per aggregate root.
- Optimistic locking: `@Version` on the root entity; the version travels in the domain object, and `save` forces
  the increment, so a change of only a child row (a delay, a ULD) still raises it. A conflict is 409
  `concurrent-change`.
- `@Transactional` is on the application service methods (reads `readOnly`); `spring.jpa.open-in-view=false`.
- No migration tool: development uses `ddl-auto=update`, the tests `create-drop` on their own `*_test` databases.

## The pieces, for `Flight`

| Piece | Package | Role |
| --- | --- | --- |
| `Flight`, `Delay` | `domain/flight` | The aggregate with its rules; `Flight.hydrate(...)` rebuilds a stored flight |
| `FlightRepository` | `domain/flight` | What the application needs: by id, by identity, by day, up to a date (retention), save, delete |
| `DbFlightRepository` | `infrastructure/flight` | Implements `FlightRepository`; maps with `fromDomain`/`toDomain` |
| `JpaFlight`, `JpaDelay` | `infrastructure/flight/jpa` | The tables `flights` and `delays`, with `@Version`, the unique key on flight number, scheduled departure date and departure airport, and the delays as `@OneToMany(cascade = ALL, orphanRemoval = true)` |
| `JpaFlightRepository` | `infrastructure/flight/jpa` | Spring Data, with `JOIN FETCH` queries (`findWithDelaysById`, …) so a flight is always loaded with its delays |

Load Planning has the same shape for `FlightCargo` (the ULDs have a composite key `JpaUldId`: the cargo and the ULD
code), and for `FlightPlanner` and `LoadPlanner`. Messaging keeps its two aggregates (`OutgoingMessage`,
`ReceivedMessage`) in `infrastructure/persistence`. The outbox (`OutboxEvent`) is a plain JPA entity in
`infrastructure/messaging`: it is technical, not part of any domain.

## Optimistic locking

```java
public void save(Flight flight) {
    JpaFlight saved = jpaFlights.save(JpaFlight.fromDomain(flight));
    if (flight.getVersion() != null) {
        // A changed delay does not touch the flights row: raise the version of the aggregate anyway.
        entityManager.lock(saved, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
    }
}
```

- `fromDomain` copies the version the aggregate was read with. If another transaction saved in between, the version
  no longer matches and Hibernate throws `ObjectOptimisticLockingFailureException`: the second change is refused,
  nothing is overwritten.
- When the root row changes too, the version goes up by two (Hibernate's own increment and the forced one).
  Harmless; only the comparison matters.
- A new aggregate has version `null`. Saving the same new instance twice gives a `DataIntegrityViolationException`;
  the use cases save a new aggregate once.
- The version stays inside the service. Events and API answers do not carry it; the pages send no version.

## Transactions

- An application service method is one transaction: load, call the aggregate, save, publish the events. The outbox
  rows are written in that same transaction (see [`messaging.md`](messaging.md)).
- One aggregate per transaction. The exceptions are deliberate: Messaging writes the inbox record and the outgoing
  messages together (that is the point of the inbox), and the retention removes all expired flights in one run.
  A planner is registered in its own transaction before the change itself.
- `open-in-view=false`: nothing is loaded lazily after the transaction. A list of a day loads the aggregates with
  `JOIN FETCH` and the controller maps them to a light DTO.

## The databases and a schema change

- One PostgreSQL instance, one database and user per context (`flight_operations`, `load_planning`, `messaging`),
  created by `init-scripts/sql/init.sql` the first time Postgres starts. The tests use `*_test` databases with
  `create-drop`.
- Development uses `spring.jpa.hibernate.ddl-auto=update`: it adds tables and columns and keeps the data (also the
  outbox and the inbox) over a restart. It never drops, renames or widens a column.
- After such a change: delete the folder `postgresql/` and restart, or run the `ALTER` by hand. The README has the
  commands for the widened flight-number columns. A real deployment would need a migration tool (Flyway or
  Liquibase); there is none.
- Derived values (ETD, ETA, status, the loading period) are never stored. The one exception is the end of the loading
  period in Load Planning (`loadingClosesAt`), so that a period that has ended stays closed.

## Adding a field

1. The field in the domain class, its `hydrate(...)` and its getter.
2. The column in the `Jpa*` entity, in `fromDomain` and in `toDomain`.
3. A save-and-read test through the repository (`DbFlightCargoRepositoryTest` shows how).
4. The DTO, if the API shows it.
5. For an existing development database: a new nullable column is added by `update`; a new `not null` column or a
   changed type needs the step above.

## References

- [Hibernate — Optimistic locking](https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#locking-optimistic)
  and [JPA `LockModeType`](https://jakarta.ee/specifications/persistence/3.2/apidocs/jakarta.persistence/jakarta/persistence/LockModeType.html).
- `README.md`: the one-time commands after a mapping change.
