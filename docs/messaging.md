# Messaging: how the services exchange events

The three services only talk to each other through RabbitMQ, and always through Messaging. This page explains how a
change becomes an event, how an event is read exactly once, what happens when something fails, and what to do
when you add an event or have to repair one.

## In short

- Every service writes its outgoing events into its own table `outbox_events`, in the same transaction as the
  change. A relay publishes them every second, in order, and only marks a row as sent after RabbitMQ confirmed it.
- Every service reads one queue with one consumer (single active consumer), so events arrive in the order they were
  sent.
- Messaging keeps every received message in an inbox (`received_messages`) and recognizes a duplicate by its id.
  Flight Operations and Load Planning have no inbox: each event sets a value, so a duplicate changes nothing.
- A message that cannot be processed is retried three times and then set aside on the service's dead letter queue,
  with a log line. Nothing is ever lost silently, and nothing resends it automatically.
- An event is JSON with `messageId`, `type`, `version` and `occurredOn`. `type` is also the routing key. A reader
  refuses a version it does not know.

## The topology

| Service | Publishes on (topic exchange) | Reads from | Dead letters |
| --- | --- | --- | --- |
| Flight Operations | `flight-operations.events` | `flight-operations.messaging-events`: `messaging.flight.scheduled`, `messaging.cpm.created` | `flight-operations.dlx` → `flight-operations.dlq` |
| Load Planning | `load-planning.events` | `load-planning.messaging-events`: the forwarded `messaging.flight.*` events and `messaging.uld-booking.received` | `load-planning.dlx` → `load-planning.dlq` |
| Messaging | `messaging.events` (to the services), `partner-exchange` (FLM, MVT, CPM) | `messaging.schedule` (SSM), `messaging.bookings` (ULD messages), `messaging.flights` (`flight-operations.flight.#`), `messaging.loads` (`load-planning.flight-cargo.#`) | `messaging.dlx` → `messaging.dlq` |
| Simulator | `network-planning.messages`, `cargo-booking.messages` | – | – |

Each service declares its own exchange, its queue with its bindings and its dead letters at start-up
(`MessagingConfig`, `Declarables`). Declaring is idempotent, so the start order does not matter, except that the
simulator needs Messaging to have declared its queues. All names are properties (`<service>.amqp.*`); the tests use
their own names, so a running service never receives test messages.

The partners: a partner service mock reads `partner-exchange` through its own queue `partner-inbox`;
Messaging also binds `partners.all` to it, to look at the partner messages in RabbitMQ.

## From a change to an event: the outbox

1. The application service saves the aggregate and hands the domain events it got back to Spring's
   `ApplicationEventPublisher`.
2. `OutboxWriter` (an `@EventListener`, so still inside the transaction) turns each event into its JSON contract
   and saves an `OutboxEvent` row: `sequence`, `message_id`, `type`, `body`, `created_at`, `sent_at`. An event that
   stays inside the context (for example `FinalizedLoadReceived`) gives no row.
3. The change and its rows are committed together, or neither is. A change therefore succeeds while RabbitMQ is
   down.
4. The relay (`OutboxRelay`, on a `@Scheduled` timer every `<service>.outbox.delay-ms` = 1000 ms; in Flight
   Operations the timer is the separate `OutboxRelayScheduler`) reads the unsent
   rows in `sequence` order, publishes each with `type` as routing key and the row's `message_id` as AMQP
   message id, and waits for the confirm of that message (`publisher-confirm-type=correlated`, inside
   `rabbit.invoke`). Only then it sets `sent_at`. On the first failure it stops, so nothing overtakes.
5. A crash after publishing but before `sent_at` publishes the row again with the same message id: at least once.
   That is why every receiver is idempotent.

Messaging has the pattern twice: `OutboxEvent` for the events to the services, and `OutgoingMessage` (an aggregate
of its domain) for the partner messages, sent by `PartnerMessageRelay` (timer: `PartnerMessageRelayScheduler`) through the port `PartnerGateway`. A partner
message carries its id, creation time and format version as AMQP properties and as the headers `message-id`,
`produced-at` and `version`; routing keys are `flm`, `mvt.ed`, `mvt.ea`, `mvt.ad`, `mvt.aa` and `cpm`.

## From a message to a use case: the listeners

- A listener (`@RabbitListener`, in `infrastructure/messaging`) reads the JSON, checks every required field by name
  and the `version`, turns it into a command and calls the application service.
- `concurrency=1` and a single active consumer per queue keep the order. Never start two instances of one service.
- Messaging first asks its `Inbox`: a message id it has seen before is ignored (`info`); an invalid message is saved
  as `REJECTED` with its reason (`warn`) and goes no further. The received message and the outgoing messages it
  causes are saved in one transaction.
- Flight Operations and Load Planning recognize a duplicate by its effect: a flight that exists is not created
  again, a ULD that is booked is not booked again, an ETD is set to a value.

## When something goes wrong

| Situation | What happens | Log |
| --- | --- | --- |
| RabbitMQ is down while a planner changes something | The change is saved; the rows wait in `outbox_events` | `error` from the relay, every second, until RabbitMQ is back |
| RabbitMQ refuses a message (for example a missing exchange) | The confirm is a nack: the row stays unsent and is tried again | `error` with the cause |
| An exchange has no queue bound | RabbitMQ confirms; nobody listens yet, which is not an error for publish/subscribe | – |
| An invalid message (bad JSON, a missing field, an unknown version, a rule it breaks) | Messaging: `REJECTED` in the inbox. The other two: rejected without requeue, so it goes to their dead letter queue | `warn` with the message id and the reason |
| A failure of ours (the database is down, a bug) | Three more attempts after 1, 2 and 4 seconds; then `SetAsideRecoverer` puts it on the dead letter queue | `error` with the message id and the cause |
| A duplicate | Ignored | `info` |

A message on a dead letter queue leaves a gap: the messages after it for the same flight are still processed, and
may be refused because they depend on it (a CPM for a flight that was never announced). Because Flight Operations
and Load Planning read in order, the gap is visible in their logs as refusals for that flight.

## Repairing a message on a dead letter queue

1. Find the cause in the log of the service: search for the message id of the `warn` or `error` line.
2. Look at the message in the RabbitMQ console (http://localhost:15672, Queues → `<service>.dlq` → Get messages,
   with "Nack message requeue true" so it stays there).
3. Repair the cause (start the database, fix the data or the code).
4. Publish the body again on the exchange it came from (`messaging.events` for Flight Operations and Load Planning)
   with the same routing key (its `type`) and the same `message_id` property. A duplicate is harmless: Messaging
   recognizes the id, the other two change nothing.
5. Remove it from the dead letter queue (Get messages with "Ack message", or Purge when all are handled).

An outbox that does not empty: `select sequence, type, created_at from outbox_events where sent_at is null order by
sequence` in the database of the service, and the relay's `error` lines say why.

## Adding an event

1. **Domain:** a record for the event in the publisher's domain; the aggregate method returns it.
2. **Contract:** a `type` of the form `<publisher>.<aggregate>.<what-happened>` (for example
   `load-planning.flight-cargo.load-finalized`), `version` 1, and an example in `contracts/<type>.json`.
3. **Publisher:** map the event to its JSON in the outbox writer of the service (`FlightEventJson.from`,
   `LoadFinalizedJson.from`, …). The relay needs no change.
4. **Routing:** if Messaging must read it, a binding on the right Messaging queue; if Messaging forwards it to Load
   Planning, also its `messaging.*` form and the binding on `load-planning.messaging-events`.
5. **Reader:** a case in the listener (or a `MessageReader` in Messaging) that checks every field and the version,
   and a command for the use case.
6. **Tests:** a `@unit` scenario for the rule, the `ContractTest` on both sides, and an `@integration` scenario that
   starts at the queue (Awaitility waits for the listener).

A change that an older reader cannot handle gets a new `version`; until every reader knows it, the publisher keeps
the old one. See [`contract-tests.md`](contract-tests.md).

## References

- [Spring AMQP — Publisher Confirms and Returns](https://docs.spring.io/spring-amqp/reference/amqp/template.html#template-confirms).
- [RabbitMQ — Single Active Consumer](https://www.rabbitmq.com/docs/consumers#single-active-consumer) and
  [Dead Letter Exchanges](https://www.rabbitmq.com/docs/dlx).
- [Chris Richardson — Transactional outbox](https://microservices.io/patterns/data/transactional-outbox.html).
- [`analysis.md`](analysis.md) §5, "Reliability": why every context has an outbox and only Messaging an inbox.
