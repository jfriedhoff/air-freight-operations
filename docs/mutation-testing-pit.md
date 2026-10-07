# Mutation testing with PIT

A green test only proves that it passes, not that it would notice a bug. PIT (pitest.org) checks
that: it changes the production code with small bugs ("mutants") and checks whether at least one
test fails because of it.

## In short

- PIT only mutates the classes in `domain` of a service and only runs its unit tests
  (`UnitAcceptanceTest` with the `@unit` scenarios, and JUnit tests in `domain`). It is set up in
  flight-operations, load-planning and messaging; the simulator has no domain.
- No Spring, database or Docker needed; a full run takes about 15 seconds.
- It is a separate Gradle task, not part of `gradlew build`. Run it before opening an MR.

## How it works

PIT makes small variants of each line, for example:

| Original | Mutant |
| --- | --- |
| `if (ata.isAfter(now))` | `if (!ata.isAfter(now))` |
| `if (!ata.isAfter(atd))` | boundary shifted, so ATA = ATD is accepted |
| `return new FlightLanded(...)` | `return null` |

For each mutant it runs the tests that reach that line:

| Result | Meaning |
| --- | --- |
| **Killed** | A test fails: good, the bug is noticed. |
| **Survived** | All tests stay green: a check is missing. |
| **No coverage** | No test reaches that line. |

- **Mutation score** = killed ÷ all mutants.
- **Test strength** = killed ÷ mutants on covered lines. This says the most about the quality of
  the tests we already have.

## Difference with line coverage

Line coverage (≥ 60 % for the evaluation) counts which lines are executed. A test without a single
`assertThat` also gets high coverage. PIT only counts a test when it actually notices a bug.

## Running it

- IntelliJ: Gradle tool window → `flight-operations-service` → Tasks → `verification` → `pitest`.
- Terminal: `./gradlew :flight-operations:flight-operations-service:pitest` (or
  `:load-planning:load-planning-service:pitest`, `:messaging:messaging-service:pitest`)
- Report: `<service>/build/reports/pitest/index.html`, e.g.
  `flight-operations/flight-operations-service/build/reports/pitest/index.html`.
  Open a class: green lines are killed, red lines survived or are not covered.

## Reading a surviving mutant

Not every survivor is a problem. For each one, ask:

1. **Does this line belong to an acceptance criterion?** Then a scenario is missing a check: add a
   `Then` step or make an existing one stricter.
2. **Is the line only used by persistence** (`hydrate`, getters for the mapper)? Then the
   integration tests cover it, not PIT.
3. **Is the mutant equivalent** (it behaves exactly like the original)? Then we ignore it.

## Settings (`build.gradle` of the service)

| Setting | Value | Why |
| --- | --- | --- |
| `targetClasses` | `...domain.*` | Unit tests only test the domain (lesson 06) |
| `targetTests` | `UnitAcceptanceTest`, `...domain.*` | Integration tests need Postgres and are too slow to run hundreds of times |
| `threads` | 4 | Faster |
| `timestampedReports` | `false` | The report is always in the same place |

There is no minimum score (`mutationThreshold`) that fails the build yet.

## Measurements

| Date | State | Mutants | Killed | Test strength |
| --- | --- | --- | --- | --- |
| 7 October 2026 | Cucumber `@unit` scenarios up to story 13 | 118 | 74 (63 %) | 84 % |
| 7 October 2026 | + `FlightTest`, `ActualTimeToleranceTest` | 118 | 88 (75 %) | 99 % |

The first measurement found these gaps; `FlightTest` and `ActualTimeToleranceTest` close them:

- The events returned by `registerDeparture`, `registerArrival`, `recordFinalizedLoad` and
  `addDelay` were never checked. The Cucumber scenarios check the state of `Flight`, and the MVT
  scenarios are `@e2e`. The events are what Messaging turns into MVT messages, so they get plain
  JUnit tests in `domain`.
- `changeDelay` only sends new estimates when the minutes change: not tested.
- `Delay.equals` could always return `true`: every scenario removed the first of its delays.
- A negative `ActualTimeTolerance` was not tested.

**Still surviving (accepted):** one mutant in `Delay.equals` that negates the `instanceof` check.
It only behaves differently for two `Delay` objects with the same id, and only `hydrate`
(persistence) creates those. The integration tests cover that.

**No coverage (29):** `hydrate`, getters only the persistence mapper uses, `equals`/`hashCode`,
`FlightPlanner` and `RetentionPeriod`. They belong to the integration tests or to stories that
have no tests yet (2, 38).
