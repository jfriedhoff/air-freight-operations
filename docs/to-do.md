# To-do: open work per team member

Work that is decided but not built yet, with enough detail to start without looking it up again.
One section per team member. Remove an item when its merge request is merged.

## In short

- Jonas: steps for the scenario that lost `@to-confirm` on 14/10/2026 (#15); it is `@wip` until the steps exist.
- Jonas: ask the coach what happens to a load that is not finalized in time (#18).

## Romeo

Nothing open.

## Jonas

### Steps for the scenarios that lost `@to-confirm`

The coach answered all five `@to-confirm` questions on 14/10/2026 (`REFLECTIE.md`, Zelfreflectie 2,
Post coaching). The tags are removed in #15, #18, #26 and #30, and in #26 the step "And the duplicate
is logged" is gone (no inbox in Load Planning). The runners used to skip these scenarios
(`"... and not @to-confirm and not @wip"`), so the steps of the scenario below do not exist yet.
It is tagged `@wip` until they do, so the build stays green; implementing one starts by removing
`@wip`. The `@to-confirm` tags of #4 and #6 are gone too: the coach answered question 9 on 15/10/2026.

| File | Change |
| --- | --- |
| `load-planning/.../features/loadplanning/15-booked-ulds-on-flight.feature` (line 59, `@unit`) | New steps: "the load of {flight} has been finalized, but the flight has not departed yet", "no load position can be assigned to it anymore", "the finalized load remains unchanged" ("... is booked without a load position" exists in `BookingSteps`) |

**After the code:**

- Remove `@wip` from the scenario, then run `UnitAcceptanceTest` and `IntegrationAcceptanceTest`
  in load-planning.
- Remove the label `to-confirm` from issues #15, #18, #26 and #30 on GitLab and update their
  Gherkin.
- Remove the line "*Scenario's met `@to-confirm`: ...*" in `REFLECTIE.md` (Zelfreflectie 2, Vragen),
  or turn it into "*Beantwoord op 14/10/2026.*".

### Question for the coach: a load that is not finalized in time (#18)

Since a loading period stays closed once it has ended, a load that is not finalized in time can
never be finalized, and Flight Operations refuses an ATD without a finalized load. So that flight can
never depart. Question 8 of the analysis named reopening as the only way out. Add the question to
`REFLECTIE.md` (Zelfreflectie 2, Vragen) and tag the coach with `@` when asking on GitLab.

## References

- `REFLECTIE.md`, Zelfreflectie 2, Post coaching: the answers of 13/10 and 14/10/2026.
- `architecture/analyse.md` §8, question 8: reopening the loading period.
- `docs/cucumber-acceptance-tests.md`: the tags and the step conventions.
