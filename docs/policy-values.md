# Policy values: changing them without a release

The project case names policy values that the operations manager can change without a new release
("Algemene regels", "Instelbare waarden"). They are in the configuration of the service that owns the
rule. Changing one takes an edit of the external configuration and a restart, no new build.

## In short

- Every value is a property under `<service>.policy.*`. The domain has no literal 5, 12, 20, 7 or 0.10.
- A value belongs to one service: the one whose rule uses it.
- "Without a release" means a restart, not a live change: the values are read once at startup.
- There is no screen for them: the operations manager is not a role in Keycloak.

## The values

| Policy value | Property | Default | Service |
| --- | --- | --- | --- |
| Tolerance for actual times | `flight-operations.policy.actual-time-tolerance` | `5m` | flight-operations |
| Retention period | `flight-operations.policy.retention-days` | `7` | flight-operations |
| Loading period opens before the STD | `load-planning.policy.loading-opens-before-departure` | `12h` | load-planning |
| Loading period closes before the ETD | `load-planning.policy.loading-closes-before-departure` | `20m` | load-planning |
| Allowed lateral balance difference | `load-planning.policy.lateral-balance-limit` | `0.10` | load-planning |

Durations take a unit (`5m`, `12h`). The balance limit is a fraction: 20% is `0.20`.

## From the property to the rule

1. `PolicyProperties` (`infrastructure/config`) reads the properties with `@ConfigurationProperties`.
2. `PolicyConfig` turns each one into a value object of the domain and offers it as a bean.
3. The application service gets the value object in its constructor and passes it to the aggregate.

| Value object | Used by |
| --- | --- |
| `ActualTimeTolerance` | `FlightService` passes it to `Flight` |
| `RetentionPeriod` | `FlightRetentionService` |
| `LoadingPolicy` | `LoadService` and `FlightInfoService` pass it to `FlightCargo` |
| `LateralBalanceLimit` | `LateralBalanceCheck` |

Spring stays in `infrastructure`: the domain only knows its own value objects. They validate the value
and hold the rule (`ActualTimeTolerance.allows`, `RetentionPeriod.lastExpiredDate`), so a unit test
builds one with any value and needs no Spring.

## Changing a value

Set the value outside the JAR and restart the service. Spring Boot lets external configuration win over
the `application.properties` inside the JAR. From highest to lowest priority:

| Way | Example |
| --- | --- |
| Command-line argument | `--flight-operations.policy.retention-days=10` |
| Environment variable | `FLIGHTOPERATIONS_POLICY_RETENTIONDAYS=10` |
| `application.properties` next to the JAR | `flight-operations.policy.retention-days=10` |

- The name of an environment variable: dots become underscores, dashes are removed, upper case.
  `FLIGHT_OPERATIONS_POLICY_RETENTION_DAYS` does not work.
- The `application.properties` in the repository is packaged in the JAR. Changing that file is a new
  build, so it only sets the defaults.

## Flights that are already stored

`FlightCargo` stores the closing time of the loading period (ETD minus the closing time), so that a
period that has ended stays closed (story #18). A new `loading-closes-before-departure` therefore does
not change flights that are already stored: they keep their closing time until their next ETD change
while the period is still open. The new value applies to flights that are announced or delayed after the
restart. The opening time is not stored: a new `loading-opens-before-departure` applies to every flight
at once.

## A missing or wrong value

| Mistake | Result |
| --- | --- |
| A negative value | The value object refuses it: the service does not start |
| A missing duration or balance limit | The value object refuses `null`: the service does not start |
| A value that cannot be read (`5 minutes`) | Spring cannot convert it: the service does not start |
| A missing `retention-days` | It becomes 0 (an `int`) and the service starts |

With a retention period of 0 the job of 00:00 UTC removes the flights of that same day. There is no
test for these cases.

## Tests

Story #37 has `37-policy-values.feature` in both services, with one example per value. "Restarted
without a new build" is a second application context of the same classes, started with only that value
changed as a command-line argument (`PolicyValueSteps`). So the test proves that external configuration
wins, not only that the default works.

## References

- [Spring Boot — Externalized Configuration](https://docs.spring.io/spring-boot/reference/features/external-config.html):
  the order in which property sources win, `@ConfigurationProperties`, and the rule for the names of
  environment variables ("Binding From Environment Variables").
- Project case, "Algemene regels": the policy values and "zonder nieuwe release".
- `architecture/analyse.md` §6 and `REFLECTIE.md` (story #37): the choice for external configuration
  and a restart.
