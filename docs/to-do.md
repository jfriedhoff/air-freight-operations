# To-do: open work per team member

Work that is decided but not built yet, with enough detail to start without looking it up again.
One section per team member. Remove an item when its merge request is merged.

## In short

- Romeo: the `@ui` scenario of #36 by hand (#36).
- Jonas: the leftovers of the `@to-confirm` answers on GitLab and in `REFLECTIE.md` (#15, #18, #26, #30).

## Romeo

### Story 36: what is left by hand

- The `@ui` scenario of #36 ("The screen shows the reason for a refusal"): register an ATD 6 minutes after the ETD
  on the details page and check that the page says a delay is needed first.

## Jonas

### Leftovers of the `@to-confirm` answers

The coach answered the `@to-confirm` questions on 14/10/2026 and 15/10/2026. The scenarios have their
steps now and no `@wip` is left. What remains is outside the code:

- Remove the label `to-confirm` from issues #15, #18, #26 and #30 on GitLab and update their Gherkin.
- Turn the line "*Scenario's met `@to-confirm`: ...*" in `REFLECTIE.md` (Zelfreflectie 2, Vragen) into
  "*Beantwoord op 14/10/2026.*".

A load that is not finalized in time is not an open question: the loading period stays closed once it
has ended (coach, 13/10/2026), so the load can no longer be finalized and Flight Operations keeps refusing
the ATD.

## References

- `REFLECTIE.md`, Zelfreflectie 2, Post coaching: the answers of 13/10 and 14/10/2026.
- `architecture/analyse.md` §8, question 8: the loading period after a delay (once closed, always closed).
- `docs/cucumber-acceptance-tests.md`: the tags and the step conventions.
