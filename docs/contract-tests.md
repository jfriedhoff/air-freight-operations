# Contract tests: one example message per event

The services only talk to each other through events on RabbitMQ. Every service tests its own side: the
publisher that it writes the event, the reader that it handles it. Without a common reference, both sides
can each be green while they no longer understand each other, for example when Flight Operations renames a
field and the tests of Messaging still send the old name. The contract tests close that gap.

## In short

- `contracts/` in the root of the repository holds one example message per event that goes from one service to
  another, named after its `type`: `flight-operations.flight.departed.json`, and so on.
- The service that **publishes** an event checks that its JSON has exactly the fields of the example.
- The service that **reads** an event checks that its reader understands the example.
- A field that one side renames, adds or drops therefore fails a test on one of the two sides, in the pipeline,
  without starting the other service.
- Plain JUnit, no Spring, database or RabbitMQ: `ContractTest` in `infrastructure/messaging` of every service.
- The examples are test data, not code: the services still share no code (analysis §5). The coach agreed that
  both sides may read the same file.

## Which events

| Example | Published by | Read by |
| --- | --- | --- |
| `messaging.flight.scheduled` | Messaging | Flight Operations (a new flight from an SSM) |
| `flight-operations.flight.announced` | Flight Operations | Messaging (FLM) |
| `flight-operations.flight.departure-estimate-changed` | Flight Operations | Messaging (MVT-ED) |
| `flight-operations.flight.arrival-estimate-changed` | Flight Operations | Messaging (MVT-EA) |
| `flight-operations.flight.departed` | Flight Operations | Messaging (MVT-AD) |
| `flight-operations.flight.landed` | Flight Operations | Messaging (MVT-AA) |
| `flight-operations.flight.removed` | Flight Operations | Messaging (removes the messages of the flight) |
| `messaging.flight.announced` | Messaging | Load Planning (a new cargo) |
| `messaging.flight.departure-estimate-changed` | Messaging | Load Planning (the end of the loading period) |
| `messaging.flight.arrival-estimate-changed` | Messaging | Load Planning |
| `messaging.flight.departed` | Messaging | Load Planning (no more bookings) |
| `messaging.flight.landed` | Messaging | Load Planning |
| `messaging.flight.removed` | Messaging | Load Planning (removes the cargo) |
| `messaging.uld-booking.received` | Messaging | Load Planning (a booked ULD) |
| `load-planning.flight-cargo.load-finalized` | Load Planning | Messaging (CPM) |
| `messaging.cpm.created` | Messaging | Flight Operations (the ATD may be registered) |

That is every event that goes from one service to another.

The messages to the partners (FLM, MVT, CPM) have no example here: the partners are outside our system, and
the `@integration` scenarios of Messaging check what it sends them.

## How it works

- `build.gradle` of every service adds `contracts/` to the test resources, so an example is on the test
  classpath as `/<type>.json`.
- `Contracts` in the root test package of every service reads an example (`example(type)`) and lists the names
  of the fields of a JSON message (`fieldsOf(json)`; a field of the elements of a list is named like
  `load[].uldCode`).
- The publisher builds the event as its outbox does (`FlightEventJson.from`, `LoadFinalizedJson.from`, the
  `CpmCreatedJson`, `FlightScheduledJson` and `UldBookingReceivedJson` records), writes it as JSON and compares
  the field names with those of the example. Only the names are compared: the values (ids, times) differ on
  every run.
- Messaging passes the flight events of Flight Operations on: its test reads the `flight-operations.flight.*`
  example, passes it on as `FlightEventService` and `OutboxWriter` do, and compares the result with the
  `messaging.flight.*` example. It also checks that `occurredOn` stays the same, because Load Planning judges the
  end of the loading period by it.
- The reader gives the example to its reader (`JsonFlightEventReader` and `JsonLoadFinalizedReader` in
  Messaging, the `CpmCreatedJson` and `FlightScheduledJson` records in Flight Operations, the `FlightEventJson`
  and `UldBookingReceivedJson` records in Load Planning) and checks the values that come out.

## Changing an event

1. Change the example in `contracts/` together with the code of the publisher.
2. The `ContractTest` of the reader then fails until its reader is adjusted too.
3. A change that an older reader cannot handle gets a new `version` (see the comment on `FlightEventJson`).

## References

- [Martin Fowler — Contract Test](https://martinfowler.com/bliki/ContractTest.html): what a contract test is
  for.
- [Pact](https://docs.pact.io/): the tool that does this between separate repositories, with a broker in the
  middle. We do not need it: all services are in one repository.
- Course slides 07 (messaging): an integration event is JSON with `messageId`, `type`, `version` and
  `occurredOn`.
