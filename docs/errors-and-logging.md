# Errors and logging: what the API answers and what the log says

Flight Operations and Load Planning answer every refused or failed request with Problem Details (RFC 9457) and a
code of their own, so a page can show the reason and a test can check it. Every service logs what went wrong at a
level that says whether someone has to act. This page lists both, and says where a new case goes.

## In short

- The domain throws `InvalidValueException` (a value that is wrong on its own: 400), `DomainRuleViolationException`
  (an action that does not fit the current state: 409) or `NotFoundException` (404). `InvalidValueException`
  extends `DomainRuleViolationException`.
- `api/ErrorHandling` turns them into Problem Details with a `code`. `api/UnexpectedErrorHandling` has the lowest
  precedence and catches everything else as a 500 `unexpected-error`, after Spring's own 404 and 405.
- No answer contains a stack trace or another internal detail. The cause is in the log.
- Log levels: `error` when our system fails (database, RabbitMQ, a bug), `warn` for input that is refused, `info`
  for a refused action and a duplicate, `debug` for an invalid value in a request.
- 401 and 403 for the role come from the security filter and have no body of ours.

## The answers of the API

| Status | `code` | When | Thrown by | Log |
| --- | --- | --- | --- | --- |
| 400 | `invalid-input` | A body that fails its DTO validation, a body that cannot be read, a value that is invalid on its own (a delay outside 1 min–12 h, an unknown delay reason, a position outside the floor plan, a description over 255 characters) | Bean Validation, Jackson, `InvalidValueException` | `debug` |
| 403 | `unknown-flight-planner`, `unknown-load-planner` | The planner in the token cannot be registered (for example a subject that is not a UUID) | `UnknownFlightPlannerException`, `UnknownLoadPlannerException` | `warn` |
| 404 | `not-found` | A flight or a flight cargo that does not exist | `NotFoundException` | `warn` |
| 409 | `rule-refused` | An action that does not fit the state: wrong status, outside the loading period, no CPM yet, an ATD or ATA too late, 12 hours of delays together | `DomainRuleViolationException` | `info` |
| 409 | `hard-check-failed` | Load Planning: finalizing while a hard check fails; the body also has the report | `HardCheckFailedException` | `info` |
| 409 | `concurrent-change` | Two changes of the same aggregate at the same moment (optimistic locking) | `OptimisticLockingFailureException` | `warn` |
| 503 | `database-unavailable` | The database cannot be reached | `CannotCreateTransactionException`, `DataAccessResourceFailureException` | `error`, with the request and the stack trace |
| 500 | `database-error` | Any other database failure | `DataAccessException` | `error`, with the request and the stack trace |
| 500 | `unexpected-error` | Anything else | `UnexpectedErrorHandling` | `error`, with the request and the stack trace |
| 401 | – | No token or an invalid one | Spring Security | – |
| 403 | – | Not exactly one of the two planner roles, or the wrong role for a change | `SecurityConfig` | – |

The `detail` is the reason the page shows. Domain reasons are in English, like all code; the page puts a Dutch
sentence before them ("Het vertrek is geweigerd: …"). Load Planning writes a reason that may repeat input on one
line in the log (`onOneLine`), so a line break in a request cannot forge a log line.

The simulator has its own small `ErrorHandling` (400 for a body it cannot read, 503 when RabbitMQ is down);
Messaging has no API.

## The log outside the API

| What | Level | Where |
| --- | --- | --- |
| A relay cannot publish (RabbitMQ down, a message refused) | `error`, every attempt, with the cause | `OutboxRelay`, `PartnerMessageRelay` |
| A scheduled run fails (for example the database) | `error` with stack trace, by Spring's scheduler | the four relays, the retention |
| A message still fails after its retries | `error` with its id and cause | `SetAsideRecoverer` |
| An invalid message (bad JSON, missing field, unknown version, a rule) | `warn` with its id and reason, once per attempt | the listeners; Messaging's `Inbox` |
| A duplicate message, a flight that already exists, a second announcement | `info` | `Inbox`, `FlightSchedulingService`, `FlightInfoService` |
| A second booking of the same ULD code by another message | `warn` | `BookingService` (Load Planning) |
| A change that succeeds | `info` with what changed | the controllers of Flight Operations and Load Planning; the services that handle a message (`FlightSchedulingService`, `FinalizedLoadService`, `BookingService`, `ScheduleService`, …); the relays and the retention |

A refused message is retried like any other failure, so its `warn` appears four times (a known limitation).

## Adding a case

- A new rule in the domain throws `DomainRuleViolationException` (409) or, for a value that is wrong on its own,
  `InvalidValueException` (400). Nothing else needs to change.
- A new kind of refusal that the page must recognize gets its own exception and a handler in `ErrorHandling` with a
  new `code`, in both services if both can give it.
- Check the answer in a test: an `@integration` scenario or a MockMvc test asserts the status and `$.code`
  (`ErrorHandlingTest`, `SpringErrorAnswersTest`).

## References

- [RFC 9457 — Problem Details for HTTP APIs](https://www.rfc-editor.org/rfc/rfc9457).
- [Spring Framework — Error Responses](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html): `ProblemDetail` and `@ControllerAdvice`.
- [`analysis.md`](analysis.md) §6, "Error handling" and "Technical errors in the log".
