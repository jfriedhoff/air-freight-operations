# Mutation testing with PIT

A green test only proves that it passes, not that it would notice a bug. PIT (pitest.org) checks
that: it changes the production code with small bugs ("mutants") and checks whether at least one
test fails because of it.

## In short

- PIT only mutates the classes in `domain` of flight-operations and only runs the unit tests
  (`UnitAcceptanceTest` with the `@unit` scenarios, and JUnit tests in `domain`).
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
- Terminal: `./gradlew :flight-operations:flight-operations-service:pitest`
- Report: `flight-operations/flight-operations-service/build/reports/pitest/index.html`.
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

## First measurement (7 October 2026, after story 13)

118 mutants, 74 killed (63 %), test strength 84 %. Survivors that show a real gap:

- The events returned by `registerDeparture`, `registerArrival`, `recordFinalizedLoad` and
  `addDelay` are never checked.
- `changeDelay` only sends new estimates when the minutes change: not tested.
- `Delay.equals` may always return `true`: every scenario has only one delay.
- A negative `ActualTimeTolerance` is not tested (story 37).
