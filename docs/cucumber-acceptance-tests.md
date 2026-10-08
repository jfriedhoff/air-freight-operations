# Cucumber acceptance tests: shared state per scenario

The acceptance criteria of the user stories are `.feature` files in
`src/test/resources/features/<context>/` and run with Cucumber. This document explains how the
step classes are built and why they share one object per scenario.

## In short

- A `@unit` scenario tests **only one aggregate** (`Flight` or `FlightCargo`): no Spring, no database,
  no mocks.
- The steps are spread over small classes per topic (delays, departure, ...).
- Those classes share the state of one scenario through **`FlightWorld`**. Picocontainer creates a
  new `FlightWorld` for every scenario and passes it to every step class.

## Why a shared object is needed

Two Cucumber rules together cause it:

1. **Step texts are global.** Cucumber looks up every Gherkin line in all step classes of the glue
   package. A text may match only one method, otherwise you get "ambiguous step".
2. **Fields are not global.** For every scenario Cucumber creates a new instance of every step
   class. A field in class A is invisible to class B.

A scenario that uses steps from two classes then gets stuck. Example from story 12:

| Step | Class | What happens |
| --- | --- | --- |
| `Given flight HJ221 is planned with ETD 06:45 UTC` | `FlightDepartureSteps` | plans the flight |
| `When I try to change the ATD` | `FlightDepartureSteps` | stores the exception |
| `Then that is refused` | `OutcomeSteps` | must check that exception |

Without a shared object the last step sees an empty `error` and the test fails, even though the
domain refused correctly. Its own `that is refused` in `FlightDepartureSteps` is not possible: story
10 uses the same text, so that becomes "ambiguous".

The alternatives are worse:

- **One big step class** for all stories: a god class of hundreds of lines.
- **`static` fields**: state leaks from one scenario into the next.
- **`cucumber-spring` for the unit tests too**: then Spring starts, and it is no longer a unit test.
- **Rewriting the Gherkin** so no text is shared: the stories are the agreement with the customer,
  we don't change them for the test code.

## How it works

```java
public class FlightDepartureSteps {
    private final FlightWorld world;

    public FlightDepartureSteps(FlightWorld world) {   // picocontainer gives every class the same FlightWorld
        this.world = world;
    }
}
```

`UnitAcceptanceTest` chooses picocontainer as object factory
(`io.cucumber.picocontainer.PicoFactory`). `IntegrationAcceptanceTest` explicitly chooses
`io.cucumber.spring.SpringFactory`: with two factories on the classpath Cucumber otherwise refuses to
start. In the integration tests a Spring bean with `@ScenarioScope` plays the same role as
`FlightWorld`.

| Class | Contents |
| --- | --- |
| `FlightWorld` | The state of one scenario: the flights, the planner ("I"), `now`, the caught `error` and the expected refusal reason. Plus `plan`, `flight` and the check "refused for the expected reason". |
| `OutcomeSteps` | Then steps that several stories share: `it/that is accepted/refused`, `the registration is accepted`, `the registration is refused`, `the registration is refused with an explanation`, `the registration is refused: ...`. |
| `FlightDelaySteps` | Stories 8, 9, 10 (delays). |
| `FlightDepartureSteps` | Story 12 (ATD). Also contains `it is now ... UTC`, which story 13 reuses. |
| `FlightArrivalSteps` | Story 13 (ATA). |

A plain `refused` still checks the reason: the When step sets `expectedRefusal` to the usual
refusal reason (e.g. "later than the ATD" for `I register ... as the ATA`), and a step like
`the registration is refused: enter a delay first` overrides it with its own reason.

`Flight` has no method to change an ATD or ATA. "I try to change the ATD" (story 12) and
"the ATA can no longer be changed" (story 13) therefore register a second time and expect
"already been registered"; story 13 then also checks that the first ATA was kept.

### Load planning

The same set-up in `be.kdg.airfreight.loadplanning.acceptance.unit`, with `CargoWorld` instead of
`FlightWorld`.

| Class | Contents |
| --- | --- |
| `CargoWorld` | One `FlightCargo` of HJ221 on a Boeing 747-400F, a loading period that is open at `NOW`, and the result of the last load check. `book` and `place` take a destination per ULD and a code (or `nextCode` for a ULD the scenario does not name). `load` books a ULD and puts it on a position; "0 kg" loads nothing, because a `Weight` of 0 does not exist. `CHECKS` holds all checks, as `PolicyConfig` gives them, and `report` the last full report. |
| `ParameterTypes` | `{weight}`: "1000 kg", "2646 lb", "1200.2 kg". |
| `LoadCheckSteps` | `the check has passed/failed`, shared by the load check stories (19, 20, 21), plus `the load is checked` (all checks) and `the soft check "..." has passed/failed`, which takes that check from the report (story 22). |
| `LateralBalanceSteps` | Story 19. |
| `OffloadingSteps` | Stories 20 and 21. A `<load>` such as "ATH on RR and UR, FRA on UL" is read by one regex step, because the rows have different shapes. "CL and CR free" stays empty; "ATH on A1, A2 and B1" continues the destination. |
| `CargoWithoutPositionSteps` | Story 22. |

The When step goes through the aggregate root (`cargo.checkLoad(...)`), as the application service
does. The floor plan is the real `Boeing747400FFloorPlan` from `infrastructure/floorplan`: it is
plain Java without Spring, and the stories name its positions (CL, B1), so a copy in the test code
could only drift from it.

### Messaging

`be.kdg.airfreight.messaging.acceptance.unit` has only one story at unit level (story 1), so there is
no World yet: `ScheduleSteps` keeps its state in its own fields. A World is added when a second step
class needs the same state (see "Adding a story", rule 4).

| Class | Contents |
| --- | --- |
| `ParameterTypes` | `{time}` ("06:30" as `LocalTime`), `{weekday}`, `{month}` and `{days}` ("5, 8, 12 and 15"). |
| `ScheduleSteps` | Story 1. What a scenario does not name gets a default (BRU to ATH, 06:30 and 09:45, SXBAA). A date with a weekday ("Monday 5 January 2026") also checks that the weekday is right, so a typo in the Gherkin cannot make a test pass. |

"The SSM is processed" calls `FlightSeries.flightsScheduled`, the domain logic that
`ScheduleService` uses after `JsonSsmReader` has read the message. Reading the JSON, the inbox and
storing the message belong to the integration tests.

## Is this still a unit test?

Yes. "Unit" refers to **what** is tested, not to how the test code is organised.

- Every When step calls one method on `Flight`; every Then step checks `Flight` or the exception it
  threw. No other production code takes part: no service, repository, database, Spring or mock.
  That is the rule from lesson 06.
- `FlightWorld` and the step classes are test code, not units. `FlightWorld` is what the fields
  filled by `@BeforeEach` are in an ordinary JUnit test class, only shared because Cucumber spreads
  the steps over several classes. Picocontainer is not Spring and only lives in the test code.
- Every scenario gets a new `FlightWorld`: scenarios stay isolated from each other.

It would be wrong with `static` fields in `FlightWorld`, or with steps that call an application
service or repository. The latter is an integration test (`@integration`).

**In one sentence:** every `@unit` scenario tests only one aggregate without Spring, database or
mocks; `FlightWorld` (or `CargoWorld`) is only the test state of one scenario, like the fields of a
JUnit test class, but shared because Cucumber spreads the steps over several classes.

## Work in progress and the TDD workflow

Every story that has a runnable scenario (`@unit` or `@integration`) has its `.feature` file in the
service it belongs to, also when it is not implemented yet. Such a file has `@wip` above `Feature:`.
Both suites skip `@wip`: a scenario without step definitions fails the build
(`UndefinedStepException`), and PIT needs a green suite.

`@wip` does not skip parsing: a `.feature` file with invalid Gherkin (e.g. a table row with a
missing cell) breaks the build of its service even when it is `@wip`.

Implementing a story:

1. Remove `@wip`. The scenarios fail: their steps are undefined. (red)
2. Write the steps. The scenarios fail because the production code is missing. (red)
3. Implement the production code until they pass. (green)
4. Refactor, run the suite and `pitest`; add checks for surviving mutants.

The `.feature` file is the source of truth for the acceptance criteria. The GitLab issue links to
it; when a criterion changes, change the file in the same MR.

Stories with only `@ui` or `@e2e` scenarios have no file: Cucumber does not run them. The
cross-cutting stories (33, 35, 36, 39) are added once security exists. Story 37 is split over
flight-operations and load-planning: each file keeps only the Examples rows of its own service.

## Adding a story

1. Remove `@wip` from the existing file (or, for a new story, put the Gherkin from the issue
   literally in `NN-short-name.feature`).
2. First check whether a step text already exists (Ctrl+click in IntelliJ). If it does, reuse it.
3. A step that several stories use goes to `OutcomeSteps` or the class of its topic, not into a
   copy.
4. State that only one class reads stays a `private` field in that class. Only when another class
   needs it does it move to `FlightWorld`.
5. Given calls the domain directly (a broken setup fails immediately); When catches the exception
   in `world.error`; a refusable When sets `world.expectedRefusal` to a fragment of the domain
   message.
6. A scenario that can't be tested yet gets `@to-confirm` and a comment line with the reason.

## Running

- In IntelliJ: right-click `UnitAcceptanceTest` → Run. Not the ▶ in the `.feature` file: that scans
  the whole classpath, finds the Spring configuration and needs a database.
- `@integration` needs `docker compose up -d`. `@e2e` and `@ui` don't run in Cucumber.
- To check whether the scenarios really notice bugs: see
  [`mutation-testing-pit.md`](mutation-testing-pit.md).
