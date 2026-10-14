# To-do: open work per team member

Work that is decided but not built yet, with enough detail to start without looking it up again.
One section per team member. Remove an item when its merge request is merged.

## In short

- Jonas: an airline code needs at least one letter (answer of the coach on 14/10/2026).
- Jonas: steps for the scenarios that lost `@to-confirm` on 14/10/2026 (#15, #26, #30); the build fails until they exist.
- Jonas: ask the coach what happens to a load that is not finalized in time (#18).

## Jonas

### Flight number: the airline code needs a letter

**The rule.** The coach answered on 14/10/2026: the airline code (two or three characters) and the
digits after it are required, and the airline code contains at least one letter. The assignment does
not say so. `221` is therefore invalid.

**How it works now.** All three services have their own `FlightNumber` with the same pattern
`[A-Z0-9]{2}\d{1,4}`. It accepts `221` (`22` + `1`) and refuses a three-letter code such as `ABC123`.

**The change.** The same pattern in all three:

```java
// IATA code (two characters, at least one letter) or ICAO code (three letters), then one to four digits
private static final Pattern FORMAT = Pattern.compile("(?:[A-Z][A-Z0-9]|[0-9][A-Z]|[A-Z]{3})\\d{1,4}");
```

`HJ221` and `3V11` (used in the #8 scenarios) stay valid, `221` and `22221` become invalid, `ABC123`
becomes valid. A three-character code is letters only, so a number such as `AB1234` can only be read
as `AB` + `1234`.

**Files to change:**

| File | Change |
| --- | --- |
| `messaging/.../messaging/domain/FlightNumber.java` | Pattern and the message ("two characters with at least one letter, or three letters, followed by one to four digits") |
| `flight-operations/.../flightoperations/domain/flight/FlightNumber.java` | Same |
| `load-planning/.../loadplanning/domain/flightcargo/FlightNumber.java` | Same |
| `messaging/.../features/messaging/29-invalid-duplicate-messages.feature` | Row "a flight number without a letter (221)" in "An invalid SSM is set aside", with its step |
| A `FlightNumberTest` per service, or the existing value-object tests (`InvalidValueTest` in flight-operations) | `HJ221`, `3V11`, `ABC123` valid; `221`, `hj221`, `HJ`, `HJ12345` invalid |

**After the code:** one line under Zelfreflectie 2 in `REFLECTIE.md` (the old question "Vluchtnummer
`221`" says the code accepts it).

### Steps for the scenarios that lost `@to-confirm`

The coach answered all five `@to-confirm` questions on 14/10/2026 (`REFLECTIE.md`, Zelfreflectie 2,
Post coaching). The tags are removed in #15, #18, #26 and #30, and in #26 the step "And the duplicate
is logged" is gone (no inbox in Load Planning). The runners used to skip these scenarios
(`"... and not @to-confirm and not @wip"`), so most of their steps do not exist yet and the build
fails on them until they are written. #4 and #6 keep `@to-confirm`: they wait for question 9 of the
analysis.

| File | Change |
| --- | --- |
| `load-planning/.../features/loadplanning/15-booked-ulds-on-flight.feature` (line 59, `@unit`) | New steps: "the load of {flight} has been finalized, but the flight has not departed yet", "no load position can be assigned to it anymore", "the finalized load remains unchanged" ("... is booked without a load position" exists in `BookingSteps`) |
| `messaging/.../features/messaging/30-date-derivation.feature` (lines 45 and 51, `@unit`) | "the moment is ... UTC" exists; "the message is invalid, because 31 April does not exist" does not (only "the message is invalid"): shorten the step in the scenario or add one in `DateDerivationSteps` |
| `load-planning/.../features/loadplanning/26-follow-flight-state.feature` (line 32, `@integration`) | New steps in load-planning: "Load Planning has already processed the event about the delay of {flight}", "the same event arrives again", "nothing changes" |

**After the code:**

- Run `UnitAcceptanceTest` and `IntegrationAcceptanceTest` in load-planning and messaging.
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
