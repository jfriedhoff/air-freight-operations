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
- A domain rule is a scenario, also an edge case the story does not mention. A technical contract or a
  returned value (an event, `equals`) is a plain JUnit test (see "Plain JUnit tests next to the
  scenarios").

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
| `FlightWorld` | The state of one scenario: the flights, the planner ("I"), `now`, the caught `error` and the expected refusal reason. `plan` (with or without times: the default schedule is STD 06:30, STA 09:45 on 9 January 2026), `depart`, `land`, `attempt` (runs a When and keeps its refusal), `assertAccepted` and `assertRefusedForTheExpectedReason`. |
| `ParameterTypes` | `{time}`: "06:45" as a `LocalTime`. `{date}`: "9 January 2026" as a `LocalDate`. `{delayKind}`: "departure" or "arrival" as a `DelayKind`. |
| `FlightSteps` | The flight before the When step: planned (with its STD, STA, ETD or ETA), departed, landed, and `it is now ... UTC`. |
| `FlightDelaySteps` | Entering, changing and removing delays, the ETD and ETA they give, and `only the delay of ... minutes remains`. |
| `FlightDepartureSteps` | The finalized load (CPM) and registering the ATD. |
| `FlightArrivalSteps` | Registering the ATA. |
| `PolicyValueSteps` | Story 37: `the operations manager sets ... to ...` creates the value object (`ActualTimeTolerance`, `RetentionPeriod`), which refuses a negative value. |
| `OutcomeSteps` | Then steps that several stories share: `it/that is accepted/refused`, `the registration is accepted/refused`, `... refused with an explanation` and `the registration is refused: ...`. |

A plain `refused` still checks the reason: the When step sets `expectedRefusal` to the usual
refusal reason (e.g. "later than the ATD" for `I register ... as the ATA`), and a step like
`the registration is refused: enter a delay first` overrides it with its own reason. A refusal
without an expected reason fails the test: every refusal has to be for the reason the scenario
causes.

`Flight` has no method to change an ATD or ATA. "I try to change the ATD" (story 12) and
"the ATA can no longer be changed" (story 13) therefore register a second time and expect
"already been registered"; story 13 then also checks that the first ATA was kept.

### Load planning

The same set-up in `be.kdg.airfreight.loadplanning.acceptance.unit`, with `CargoWorld` instead of
`FlightWorld`.

| Class | Contents |
| --- | --- |
| `CargoWorld` | One `FlightCargo` of HJ221 on a Boeing 747-400F, a loading period that is open at `NOW`, and the scenario state: `now`, `checkResult` (the check the Then steps read), `report` (all checks), `error`, `expectedRefusal` and `acted`. Flight helpers: `goesTo`, `departsAt` (a new cargo: the stored closing time of the loading period does not follow a new STD, so call it before anything is booked), `delayDepartureBy` (through `changeEstimatedDeparture`, entered at `now`), `assertFlight`, `assertWithinLoadingPeriod`. Cargo helpers: `book`, `bookOn` (on one of the flights, the weight read inside the call so that a refused weight is refused there), `place`, `load` (book and place), `loadWeightOn` (a ULD for ATH; "0 kg" loads nothing, because a `Weight` of 0 does not exist) and `nextCode` for a ULD the scenario does not name. `ANY_WEIGHT` for a ULD whose weight the scenario does not name, `CHECKS` for all checks as `PolicyConfig` gives them. Plus `attempt`, `assertAccepted` and `assertRefusedForTheExpectedReason`, as in `FlightWorld`. |
| `ParameterTypes` | `{uld}`: "AKE16689R7" as a `UldCode`. `{weight}`: "1000 kg", "2646 lb", "1200.2 kg", "-1 kg". `{moment}`: "9 January 2026 06:30" or "8 January 18:29" (2026) as an `Instant`. |
| `FlightSteps` | The flight and the moment of the scenario: STD, destination, departure delays (`afterwards ...` enters or removes a delay at the moment the scenario named before), `it is ... UTC` (a refusal then has to be for the loading period) and `it is ... UTC, within the loading period`. |
| `BookingSteps` | Cargo that is booked but has no load position, a flight without cargo, booking with a weight, booking the same code again (a refusal then has to be for the double booking), a ULD of a weight (a refusal then has to be for the weight) and `there is still one booking of ... with ...`. Story 15 uses `... is booked without a load position` as a Then: add the `acted` check of `LoadPositionSteps.isOn` then. |
| `CargoOnBoardSteps` | Cargo that is already on a load position before the When step: weights per side or position, named ULDs for a destination, `these ULDs are on board:` (a table with ULD, destination and position), `<load>` descriptions and `... is already on ...` (a refusal then has to be for that occupied position). |
| `LoadDescription` | Plain Java, no Cucumber: reads a `<load>` such as "ATH on RR and UR, FRA on UL" into ULDs. "CL and CR free" stays empty; "ATH on A1, A2 and B1" continues the destination. One regex step uses it, because the rows have different shapes. |
| `LoadPositionSteps` | Placing a ULD and clearing it. "AKE16689R7 is on CL" is both a Given and a Then: Cucumber ignores the keyword, so one method places the ULD before the When and checks it after the When (`acted`, set by `attempt`). A position that is not on the floor plan has to be refused for that reason. `it is recorded that I assigned/cleared ...` checks the ULD, not the event. |
| `FinalizeLoadSteps` | `the load has been finalized` (a refusal then has to be for the finalized load) and `the load can still be finalized if all hard checks pass`. `it is recorded that I finalized the load` checks the cargo, not the event. |
| `PolicyValueSteps` | Story 37: `the operations manager sets ... to ...` creates `LoadingPolicy` or `LateralBalanceLimit`. The other time of the loading period keeps its default, so each time is checked on its own. |
| `OutcomeSteps` | `it/that is accepted/refused` and `it/that is refused with an explanation`, as in flight-operations. |
| `LoadCheckSteps` | All checks together: `the load is checked` (also `I click Check Load`), `the check has passed/failed`, `the soft check "..." has passed/failed` (which also selects that check in the report) and `I see this report:` (the table compared as a whole, in the order of `CHECKS`, so a missing or extra check fails too). |
| `LateralBalanceSteps` | The lateral balance check: its When and its report text. |
| `OffloadingSteps` | The offloading check: its When and its report text, for one ULD in the way or as a whole (`the report states:` with a doc string). |
| `AllCargoLoadedSteps` | The soft check "not all cargo loaded": its report text. It only runs with all checks together. |

The When step goes through the aggregate root (`cargo.checkLoad(...)`), as the application service
does. The floor plan is the real `Boeing747400FFloorPlan` from `infrastructure/floorplan`: it is
plain Java without Spring, and the stories name its positions (CL, B1), so a copy in the test code
could only drift from it.

### Messaging

`be.kdg.airfreight.messaging.acceptance.unit` has no World yet: every step class only reads its own
state (story 1 in `ScheduleSteps`, story 30 in `DateDerivationSteps`), so they keep it in their own
fields. A World is added when a second step class needs the same state (see "Adding a story", rule 4).
`ParameterTypes` is in `acceptance.common`: both suites have it in their glue, because the unit and
integration steps use the same dates and times.

| Class | Contents |
| --- | --- |
| `ParameterTypes` (`common`) | `{time}` ("06:30" as `LocalTime`), `{hhmm}` ("1200", a telegram time), `{day}` ("09", a day of the month: `{int}` cannot read the leading zero), `{date}` ("9 January 2026"), `{moment}` ("9 January 2026 08:00", UTC), `{weekday}`, `{month}` and `{days}` ("5, 8, 12 and 15"). |
| `ScheduleSteps` | Story 1. What a scenario does not name gets a default (BRU to ATH, 06:30 and 09:45, SXBAA). A date with a weekday ("Monday 5 January 2026") also checks that the weekday is right, so a typo in the Gherkin cannot make a test pass. |
| `DateDerivationSteps` | Story 30. Calls `DateDerivation` directly, as `BookingService` does. "The message is invalid" checks the refusal of the derivation: that is what makes `BookingService` set the message aside. The distance scenario also checks the day counts in its Gherkin. |

"The SSM is processed" calls `FlightSeries.flightsScheduled`, the domain logic that
`ScheduleService` uses after `JsonSsmReader` has read the message. Reading the JSON, the inbox and
storing the message belong to the integration tests.

### Simulator

The simulator has no domain, so no `@unit` scenarios: its unit tests are plain JUnit
(`ScheduleRecordTest`, `BookingRecordTest`, `SimulatorServiceTest`). The `@integration` scenarios run
through the API with MockMvc and read the sent message back from a real RabbitMQ.

| Class | Contents |
| --- | --- |
| `SimulatorWorld` | A Spring bean with `@ScenarioScope`: posts JSON with MockMvc and keeps every response of the scenario. |
| `StreamProbe` (test support, not a step class) | A queue on each exchange. `nextSsm` and `nextUldMessage` read what was sent, `nothingSent` checks that nothing was, `clear` empties the queues before a scenario. |
| `FlightScheduleSteps`, `BookingSteps` | Story 31 and story 32: supplying records and the structure of the sent message, compared strictly with the contract. |
| `OutcomeSteps` | What both stories share: unreadable JSON (posted to both endpoints), the status of the answer, nothing sent, and the production profile. |

A Spring profile is fixed when the context starts. The "Not in production" scenarios have the tag
`@production` and run in their own suite, `ProductionAcceptanceTest`, with `ProductionCucumberConfig`
(`@ActiveProfiles("production")`) and the same step classes. That is why the steps are in
`acceptance.steps` and each context configuration in its own package: a glue path may hold only one
`@CucumberContextConfiguration`.

The tests use their own exchanges (`src/test/resources/application.properties`), so a Messaging
service that happens to run does not receive test messages. The `@e2e` scenarios are run by hand with
`simulator/simulator-service/http/simulator.http`.

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

## Plain JUnit tests next to the scenarios

Next to the `@unit` scenarios, `src/test/java/.../domain/` has plain JUnit 5 + AssertJ tests, with the
same limits: no Spring, database or mocks. Which of the two a test is follows one rule:

- **A domain rule is a scenario**, whether or not the story already mentions it. An edge case nobody
  wrote down (a weight of 0, a negative policy value, the same ULD twice on one flight) is still a rule
  of the domain, so it gets a scenario in the story it belongs to.
- **A technical contract or a returned value is a JUnit test**: `equals`, the event an aggregate returns
  (what the application service publishes and Messaging turns into a message), the exception type that
  decides the HTTP status, a getter that only the application service reads.

A Then step checks the state of the aggregate, not the event it returned. The one exception is
"it is recorded that I removed the delay" (story 10): after removing, the delay is gone, so the event
is the only place that says who removed it.

PIT runs both and shows what neither checks (see [`mutation-testing-pit.md`](mutation-testing-pit.md)).

| Service | Test | What it checks |
| --- | --- | --- |
| flight-operations | `FlightTest` | The events of `addDelay`, `changeDelay` (none when only the description changes), `recordFinalizedLoad`, `registerDeparture` and `registerArrival` (the MVT scenarios are `@e2e`), and `isFinalizedLoadReceived`, which `FinalizedLoadService` reads to ignore a second CPM |
| flight-operations | `InvalidValueTest` | Which exception a refusal is: an invalid value (400) or an action that does not fit the state (409) |
| flight-operations | `DelayCodeCatalogTest` | What the catalog returns for a known code, and that an entry of the IATA list needs a description |
| flight-operations | `FlightPlannerIdTest` | The planner id taken from the account, and the message of the refusal for an unregistered planner |
| load-planning | `FlightCargoTest` | The events of `book`, `assign`, `clear` and `finalizeLoad` (only the ULDs with a position are in the load of the CPM), and `hasBooked` |
| load-planning | `UldTest` | `equals`: a ULD is identified by its code |

A JUnit test that needs a package-private constructor (`new Uld(...)` in `UldTest`) is in the same
package as the class.

## Integration tests

An `@integration` scenario tests one deployment unit from its entry point to its database: a REST
endpoint (through MockMvc) or, for a message, its queue. It runs with `IntegrationAcceptanceTest`
and `SpringCucumberConfig` (`@SpringBootTest` + `@AutoConfigureMockMvc`). So far this is set up in
messaging (story 30) and load-planning (story 15); flight-operations follows the same pattern.

| Part | What it does | Why |
| --- | --- | --- |
| `src/test/resources/application.properties` | Replaces the main `application.properties` during the tests: the same settings, except that Hibernate uses its own schema `test` (`default_schema`, `create_namespaces`, `ddl-auto=create-drop`). | The tests run against PostgreSQL and RabbitMQ from Docker Compose, as in production, but never touch the dev data (lesson 06: an isolated, reproducible test database). |
| `DatabaseCleanUp` | An `@After` hook that empties the tables after every scenario. | Every scenario starts empty, like `TestHelper.cleanUp()` in the course. |
| `Clock` as `@MockitoBean` | Declared in `SpringCucumberConfig`; a step sets the time with `given(clock.instant())`. | System time is outside our code, and a scenario can need two moments ("processed on 2 January", "arrives again on 25 January"). |
| Awaitility | `await().atMost(...).untilAsserted(...)` in the Then steps. | A message is published on the exchange of its sender, as the real sender does, and the listener handles it on its own thread. The test waits for the result instead of sleeping. |

"Nothing happened" cannot be awaited. `UldMessageSteps` (story 30) therefore publishes a second,
new message on the same queue after the duplicate: the queue has one consumer, so messages are
handled in order, and once the second one is booked, the duplicate has been handled too. What
Messaging passes on to the other units is checked in its outbox.

Load-planning (story 15) uses the same idea for every message it sends:

| Part | What it does | Why |
| --- | --- | --- |
| `MessagingEvents` | Plays Messaging: sends flight events and ULD bookings on `messaging.events`, in Messaging's JSON envelope. `waitUntilHandled()` then announces a follow-up flight and waits until it exists. `assertSetAside(id)` reads `load-planning.dlq`. | One way of waiting for all steps, also when the message is refused (a refused message is retried three times first, so the timeout is 30 s). |
| `MessageWorld` | The state of one scenario (`@Component @ScenarioScope`): the flight, the last message id, the reason it must be refused for. | Several step classes share it. Spring hands them a proxy, so always use its methods: a field read through the proxy is `null`. |
| `LogCapture` | A Logback `ListAppender` on `be.kdg.airfreight.loadplanning` for the duration of a scenario. | For the steps "... is logged". |

Two things to know before running the load-planning integration tests:
- **Stop the load-planning service.** Its queue allows a single active consumer: while the service runs, the
  listener of the test stays idle and every scenario times out.
- `assertSetAside` takes dead letters until it finds its own, so older dead letters on `load-planning.dlq`
  (from development or earlier runs) are dropped.

The other services, after stories 2, 3, 26 to 29, 37 and 38:

| Part | Where | What it does and why |
| --- | --- | --- |
| `MessagingTraffic`, `Messages`, `ScenarioTime` | messaging | Plays the senders on their exchanges and reads what Messaging stored: the inbox, the outbox for the other units, the partner messages. `Messages` builds valid messages that a scenario changes or breaks; `ScenarioTime` gives the mocked `Clock` the time of the stories before every scenario. |
| Counting before and after | messaging | A queue of the development environment can still hold older messages, which the listeners process first. The steps first let the queue empty (`waitUntilHandled`), then count, so only the message of the scenario can change the count. |
| Own exchanges and queues | all four services | Every service reads the names of its exchanges and queues from `AmqpProperties` (`<service>.amqp.*`). Production keeps the names in `main/resources/application.properties`; the test profile gives each one the name `<service>-test.` + the production name, such as `load-planning-test.load-planning.messaging-events`. So a service that runs at the same time never takes a test message (load-planning reads with a single active consumer: a running service would get them all), a test event never reaches another service, and the tests never declare the exchanges and queues of a running service. Before, the events of the messaging tests stayed in the queues of Load Planning: the next run of the load-planning tests handled them first, and a service started later treated them as real. |
| Calling the use case | flight-operations | The tests of Flight Operations read nothing from RabbitMQ (see its test profile), and `DddArchTest` lets only the listener call a use case from infrastructure. So the steps of story 2 call `FlightSchedulingService`, as `MessagingEventListener` does. |
| `TestClock` | flight-operations, load-planning | A `@Primary` clock that a scenario can set (story 38: "it is 16 January 2026 00:00 UTC"); otherwise the system time. |
| `RabbitMqOutageSteps` | flight-operations | Story 28: points the connection factory at a port where nothing listens (`setAddresses`: Spring Boot passes an address list, which wins over the port) and puts it back after the scenario. |
| A restarted unit | flight-operations, load-planning | Story 37: a second application context of the same classes, started with only the changed value as a command-line argument (that wins over `application.properties`; `SpringApplicationBuilder.properties` would not). In load-planning it gets `ddl-auto=none`, because the test profile drops the schema when a context closes. |

The messaging tests drain `partners.all` the way `assertSetAside` drains the dead letter queue: older partner messages are dropped.

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

Stories with only `@ui` or `@e2e` scenarios have a file too, so the issue can link to it; Cucumber
does not run them. A story that spans several services (7, 34, 36, 37, 39) is split: each service's
file keeps only the scenarios or Examples rows it answers, and says in a comment where the rest is.

## Adding a story

1. Remove `@wip` from the existing file (or, for a new story, put the Gherkin from the issue
   literally in `NN-short-name.feature`).
2. First check whether a step text already exists (Ctrl+click in IntelliJ). If it does, reuse it.
   A text that means the same as an existing one gets a second annotation on that method
   (`@Given("the load is checked")` and `@Given("I click Check Load")`): Cucumber annotations are
   repeatable. A step method never calls another step method; shared code goes into a private
   method or the World.
3. A step goes to the class of its topic (the flight, booking, a load check, ...), not to a class
   per story: stories reuse each other's steps. `OutcomeSteps` holds the accepted/refused steps.
4. State that only one class reads stays a `private` field in that class. Only when another class
   needs it does it move to the World.
5. Given calls the domain directly (a broken setup fails immediately); When runs through
   `world.attempt(...)`, which keeps the refusal in `world.error`. The step that causes a refusal
   sets `world.expectedRefusal` to a fragment of the domain message: usually the When, in
   load-planning often the Given that blocks the change (the loading period, an occupied position,
   a finalized load).
6. A scenario that can't be tested yet gets a tag and a comment line with the reason: `@wip` when
   the production code it needs is missing (e.g. a REST endpoint), `@to-confirm` only when it waits
   for an answer (how to read the criterion).

## Running

- In IntelliJ: right-click `UnitAcceptanceTest` → Run. Not the ▶ in the `.feature` file: that scans
  the whole classpath, finds the Spring configuration and needs a database.
- `@integration`: run `docker compose up -d`, **stop the service itself** (it would read from the same
  queue), then right-click `IntegrationAcceptanceTest` → Run. While the tests run, the outbox relays
  really publish on the exchanges. `@e2e` and `@ui` don't run in Cucumber.
- To check whether the scenarios really notice bugs: see
  [`mutation-testing-pit.md`](mutation-testing-pit.md).
- In GitLab the same suites run in the pipeline, every service with its own database and RabbitMQ:
  see [`ci-pipeline.md`](ci-pipeline.md).

## References

- Course slides 06 (Spring testing): "Unit tests", "Integration tests" and "TestHelper".
- [Cucumber — Sharing state between steps](https://cucumber.io/docs/cucumber/state/): why step
  classes need a shared object, and dependency injection as the way to get one.
- [Cucumber — Gherkin reference](https://cucumber.io/docs/gherkin/reference/): `Scenario Outline`,
  `Examples`, tags.
- [Cucumber — API reference](https://cucumber.io/docs/cucumber/api/): step definitions, hooks, tags
  and custom parameter types.
- [cucumber-picocontainer](https://github.com/cucumber/cucumber-jvm/tree/main/cucumber-picocontainer),
  [cucumber-spring](https://github.com/cucumber/cucumber-jvm/tree/main/cucumber-spring) and
  [cucumber-junit-platform-engine](https://github.com/cucumber/cucumber-jvm/tree/main/cucumber-junit-platform-engine):
  the object factories of the two suites and how a suite selects its glue, tags and factory.
- [Awaitility — Usage](https://github.com/awaitility/awaitility/wiki/Usage): waiting for the result
  of a listener.
- `architecture/analyse.md` §9: the test strategy and the test levels.
