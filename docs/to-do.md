# To-do: open work per team member

Work that is decided but not built yet, with enough detail to start without looking it up again.
One section per team member. Remove an item when its merge request is merged.

## In short

- Jonas: keep a loading period closed once it has ended (story #18, answer of the coach on
  13/10/2026).

## Jonas

### Loading period: once closed, always closed (#18)

**The rule.** The coach answered question 8 of the analysis on 13/10/2026:

- A departure delay does not reopen a loading period that has ended.
- A departure delay that arrives while the period is still open moves its end.
- Removing a delay moves the end back, and so can close an open period.

**How it works now.** Nothing about the loading period is stored. On every action (place, clear,
finalize) `FlightCargo.requireLoadCanChange` computes the period from the copy of the flight:
start = STD − 12 h, end = current ETD − 20 min. The code has no memory that the end has passed, so a
later ETD gives a later end and the period opens again.

**The change.** `FlightCargo` stores the closing time. It only moves while the period is still open.

```java
private Instant loadingClosesAt;          // at creation: STD − 20 min

public void changeEstimatedDeparture(Instant etd, Instant now, LoadingPolicy policy) {
    if (now.isBefore(loadingClosesAt)) {
        loadingClosesAt = etd.minus(policy.closesBeforeEstimatedDeparture());
    }
    flightInfo = flightInfo.withEstimatedDeparture(etd);
}

public LoadingPeriod loadingPeriod(LoadingPolicy policy) {
    return new LoadingPeriod(flightInfo.std().minus(policy.opensBeforeScheduledDeparture()), loadingClosesAt);
}
```

The check on each action stays `loadingPeriod(policy).contains(now)`; it now reads the stored end.

**Files to change** (all in `load-planning/load-planning-service`):

| File | Change |
| --- | --- |
| `domain/flightcargo/FlightCargo.java` | Field `loadingClosesAt`, `changeEstimatedDeparture`, `loadingPeriod` reads the field, `create` gets the policy, `hydrate` gets the closing time, a getter |
| `application/FlightInfoService.java` | Constructor gets `LoadingPolicy` and `Clock`; `flightAnnounced` passes the policy to `create`; `departureEstimateChanged` calls `changeEstimatedDeparture` with `clock.instant()` |
| `infrastructure/flightcargo/jpa/JpaFlightCargo.java` | Column `loadingClosesAt`, in `fromDomain` and `toDomain` |
| `features/loadplanning/18-assign-load-position.feature` | Scenario 1 becomes "A departure delay does not reopen a loading period that has ended" with "Then that is refused with an explanation"; both scenarios lose `@to-confirm` |
| `acceptance/unit/CargoWorld.java` | `delayDepartureBy` calls `changeEstimatedDeparture`; `departsAt` rebuilds the cargo, because the stored closing time does not follow a new STD; every `FlightCargo.create` gets the policy |
| `acceptance/unit/FlightSteps.java` | Four new steps: "there is no departure delay and the load has not been finalized", "there is a departure delay of {int} minutes", "afterwards a departure delay of {int} minutes is entered", "afterwards that delay is removed" |
| Other tests that call `FlightCargo.create` | The extra policy parameter |

**Effects to know and to explain:**

- Policy values (#37): the 20 minutes is part of the stored closing time. After a new value and a
  restart, flights that are already stored keep their closing time until their next ETD change. A new
  value applies to flights that are announced or delayed after the restart.
- The local `load_planning` database: rows that exist have no closing time. Empty the table once.
- The moment that counts is the moment Load Planning processes the event, not the moment the flight
  planner entered the delay. A delay entered just before the end and processed just after it comes too
  late. Using `occurredOn` of the message would close that gap.

**Open question for the coach.** Finalizing is only allowed within the loading period, and Flight
Operations refuses an ATD without a finalized load. With this rule a load that is not finalized in
time can never be finalized, so the flight can never depart. Question 8 of the analysis named
reopening as the only way out.

**After the code:**

- Run `UnitAcceptanceTest` and `pitest` for load-planning; add the PIT numbers to
  `docs/mutation-testing-pit.md`.
- Update the Gherkin of issue #18 on GitLab and remove the label `to-confirm` when its last
  `@to-confirm` scenario is gone.
- Remove the sentence "De code berekent het einde nog telkens uit de huidige ETD ..." from
  `REFLECTIE.md` (Zelfreflectie 2, Post coaching).
- `docs/policy-values.md`: add the effect on stored flights.

## References

- `architecture/analyse.md` §8, question 8, and §7.2, invariant 6: the rule and the answer of the
  coach.
- `REFLECTIE.md`, Zelfreflectie 2, Post coaching: the answer of 13/10/2026.
- [Issue #18](https://gitlab.com/kdg-ti/software-architecture/teams-26-27/romeo-jonas/air-freight-operations/-/issues/18):
  the story and its acceptance criteria.
- `docs/cucumber-acceptance-tests.md`: the tags and the step conventions the new steps follow.
