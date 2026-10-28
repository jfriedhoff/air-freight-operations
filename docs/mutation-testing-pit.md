# Mutation testing with PIT

A green test only proves that it passes, not that it would notice a bug. PIT (pitest.org) checks
that: it changes the production code with small bugs ("mutants") and checks whether at least one
test fails because of it.

## In short

- PIT only mutates the classes in `domain` of a service and only runs its unit tests
  (`UnitAcceptanceTest` with the `@unit` scenarios, and JUnit tests in `domain`). It is set up in
  flight-operations, load-planning and messaging; the simulator has no domain.
- No Spring, database or Docker needed; a full run takes about 15 seconds.
- It is a separate Gradle task, not part of `gradlew build`. Run it before opening an MR. In GitLab it is a
  manual job per service in the stage `mutation` (see [`ci-pipeline.md`](ci-pipeline.md)).

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

1. **Is the line a domain rule?** Then a scenario is missing a check: add a `Then` step, make an
   existing one stricter, or add a scenario to the story the rule belongs to, also for an edge case
   the story does not mention. **Is it a technical contract or a returned value** (`equals`, an
   event)? Then it gets a plain JUnit test (see "Plain JUnit tests next to the scenarios" in
   [`cucumber-acceptance-tests.md`](cucumber-acceptance-tests.md)).
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
| 14 October 2026 | Domain rules from JUnit to scenarios (stories 10, 37) | 126 | 99 (79 %) | 99 % |
| 22 October 2026 | + `Flight.removed` (story 38), no new unit tests | 127 | 99 (78 %) | 99 % |
| 22 October 2026 | + the events of `announced` and `removed` in `FlightTest` | 127 | 101 (80 %) | 99 % |

The first measurement found these gaps; `FlightTest` and `ActualTimeToleranceTest` close them:

- The events returned by `registerDeparture`, `registerArrival`, `recordFinalizedLoad` and
  `addDelay` were never checked. The Cucumber scenarios check the state of `Flight`, and the MVT
  scenarios are `@e2e`. The events are what Messaging turns into MVT messages, so they get plain
  JUnit tests in `domain`.
- `changeDelay` only sends new estimates when the minutes change: not tested.
- `Delay.equals` could always return `true`: every scenario removed the first of its delays.
- A negative `ActualTimeTolerance` was not tested.

On 14 October 2026 the domain rules moved from JUnit to scenarios: "Removing one of several delays"
(story 10) now removes the first and the later delay and checks which one remains, which kills the
`Delay.equals` mutant that `FlightTest` killed before. Story 37 has a `@unit` outline for a negative
tolerance and retention period, which replaces `ActualTimeToleranceTest`. The duplicate of story 08
left `DelayCodeCatalogTest`.

**Still surviving (accepted):** one mutant in `Delay.equals` that negates the `instanceof` check.
It only behaves differently for two `Delay` objects with the same id, and only `hydrate`
(persistence) creates those. The integration tests cover that.

**No coverage (25):** `hydrate`, getters only the persistence mapper uses, `equals`/`hashCode`,
`notFound`, `FlightPlanner` and `RetentionPeriod`. They belong to the integration tests or to
stories that have no unit tests yet (2, 38).

### Load planning

| Date | State | Mutants | Killed | Test strength |
| --- | --- | --- | --- | --- |
| 7 October 2026 | Cucumber `@unit` scenarios of story 19 | 157 | 53 (34 %) | 78 % |
| 7 October 2026 | + story 20 | 163 | 85 (52 %) | 85 % |
| 7 October 2026 | + story 21, `OffloadingCheckTest` | 163 | 89 (55 %) | 86 % |
| 7 October 2026 | + story 22 | 163 | 102 (63 %) | 85 % |
| 7 October 2026 | + story 18 | 163 | 120 (74 %) | 90 % |
| 14 October 2026 | Loading period stays closed (story 18) | 165 | 128 (78 %) | 92 % |
| 14 October 2026 | + stories 15, 20, 21, 37 for the rules without a scenario, `FlightCargoTest`, `UldTest` | 165 | 139 (84 %) | 99 % |
| 15 October 2026 | Rerun | 180 | 155 (86 %) | 99 % |
| 22 October 2026 | Rerun | 191 | 166 (87 %) | 99 % |

Every mutant in `LateralBalanceCheck` is killed. The 15 survivors are in code that story 19 passes
through but does not check: `book` and `assign` (stories 15, 18) and the validation of
`LoadingPolicy`, `LateralBalanceLimit`, `Weight` and `AircraftType` (37). `Uld.hasPosition`
returning `true` also survives: the lateral balance check asks `isOnSide`, which already skips a
ULD without a position. Story 22 (cargo without a position) will check it. The score rises as the
other load-planning stories get their steps.

Story 21 and `OffloadingCheckTest` kill every mutant in `OffloadingCheck` except `name()`, which no unit
test reads. `OffloadingCheckTest` is plain JUnit for the report with several ULDs involved, which no
scenario checks.

Story 22 kills the `Uld.hasPosition` survivor and every mutant in `AllCargoLoadedCheck` except `name()`.
Its last step finalizes the load, which brings new survivors in `finalizeLoad`, `isFinalized` and
`hardChecksPassed`: story 22 only checks that finalizing works, story 24 checks that it is refused.

Story 18 kills every survivor in `assign` and `clear`: the loading period, the occupied position,
`FloorPlan.contains`, `LoadingPeriod.contains`, `Uld.isAt` and the returned events. Still surviving:
finalizing (story 24), `book` (story 15), the validation of `Weight`, `LoadingPolicy` and
`LateralBalanceLimit` (story 37), and `Uld.hashCode` returning 0 and one `Uld.equals` mutant. The
`Uld.equals` mutant only behaves differently for two `Uld` objects with the same code, which only
`hydrate` creates (the same as `Delay.equals` in flight-operations).

The stored closing time of the loading period adds `changeEstimatedDeparture` and
`getLoadingClosesAt`. The two scenarios "A departure delay does not reopen a loading period that has
ended" and "Removing a delay closes the loading period" kill the mutant of the `if` in
`changeEstimatedDeparture`. `getLoadingClosesAt` has no coverage: only the JPA mapping reads it, as with
`getVersion`.

The last survivors were rules without a scenario and returned values without a check. The rules got
scenarios, and `OffloadingCheckTest` moved into them:

| Survivor | Now killed by |
| --- | --- |
| The duplicate check in `book` returning `false` | Story 15, "The same ULD code twice on one flight is refused" (the integration scenario never reaches the domain: `BookingService` stops the duplicate first) |
| `Weight`: `<= 0` becomes `< 0` | Story 15, "A ULD always weighs something" |
| The validation of `LoadingPolicy` and `LateralBalanceLimit` | Story 37, "A policy value cannot be negative" |
| The report with several ULDs (was `OffloadingCheckTest`) | Stories 20 and 21, a scenario each with a table of ULDs and the whole report |
| `book` returning `null`, `hasBooked` returning `true` | `FlightCargoTest`, which also checks the events of `assign`, `clear` and `finalizeLoad` (the Then steps of stories 18 and 24 now only check the state) |
| `Uld.equals` | `UldTest` |

**Still surviving (accepted):**

- `Uld.hashCode` returning 0 is equivalent: equal ULDs still get the same hash, a `HashSet` only
  gets slower. Killing it would need a test of how the hash is computed.
- The negated `while (foundMore)` in `OffloadingCheck.check` loops forever. PIT reports it as
  **timed out**, which counts as killed.
- `LateralBalanceCheck.heavierOf`: `comparison > 0` becoming `>= 0` is equivalent. The line above
  already returns `null` when both sides weigh the same, so `comparison` is never 0 there.

**No coverage (23):** `hydrate`, getters only the persistence mapper uses, `equals`/`hashCode`,
`notFound`, the `name()` of the checks, `LoadPlanner`, and `FlightInfo.withEstimatedArrival`, which
only `FlightInfoService` calls.

### Messaging

| Date | State | Mutants | Killed | Test strength |
| --- | --- | --- | --- | --- |
| 7 October 2026 | Cucumber `@unit` scenarios of story 1 | 86 | 12 (14 %) | 100 % |
| 22 October 2026 | + story 30 (date derivation) | 92 | 32 (35 %) | 100 % |
| 22 October 2026 | + `@unit` scenarios of stories 11, 14, 25, 28 and 29, `MvtWriterTest`, `UldMessageTest`, `LoadFinalizedTest` | 92 | 68 (74 %) | 100 % |

Every mutant in `FlightSeries` and `SchedulePeriod` is killed, including the arrival at or before the
departure time (next day) and the last day of the period. Story 30 kills all 12 mutants in
`DateDerivation`, and there are no survivors.

On 22 October 2026 the MVT writers, the validation of a ULD message, a CPM line and the format
version, and `markSent` got their tests:

| What had no coverage | Now killed by |
| --- | --- |
| The MVT writers (15 mutants): stories 11 and 14 only had `@e2e` scenarios | The `@unit` scenarios after midnight in stories 11 and 14, and `MvtWriterTest` for `writes` |
| The validation of `UldMessage` and `FormatVersion` | Story 29, "A ULD message with an invalid value is invalid" (replaces `IncomingMessageRulesTest`, which held the same rules as JUnit) |
| The validation of `CpmLine` | Story 25, "A ULD line in the CPM needs a ULD code, a load position and a weight unit" |
| `OutgoingMessage.markSent` and `isSent` | Story 28, "A message that has been sent is not marked as sent again", and `MvtWriterTest` |
| `UldMessage.bookingOn`, `LoadFinalized.cpmCreated` | `UldMessageTest`, `LoadFinalizedTest` |

**No coverage (24):** `ReceivedMessage` (17), and `hydrate`, `equals` and `hashCode` of
`OutgoingMessage`, which the integration tests cover.

## References

- [PIT — Basic concepts](https://pitest.org/quickstart/basic_concepts/): mutants, killed, survived,
  equivalent mutants.
- [PIT — Mutators](https://pitest.org/quickstart/mutators/): which changes PIT makes.
- [Gradle plugin for PIT](https://gradle-pitest-plugin.solidsoft.info/): the settings in
  `build.gradle`.
- [pitest-junit5-plugin](https://github.com/pitest/pitest-junit5-plugin): lets PIT run JUnit 5
  tests, and Cucumber through them.
- Course slides 06 (Spring testing), "Unit tests": unit tests only test the domain.
