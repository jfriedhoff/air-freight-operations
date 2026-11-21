# Hand checks for the `@e2e` and `@ui` scenarios

Scenarios tagged `@e2e` cross several services, and `@ui` scenarios need a browser, so Cucumber never runs them (see
`cucumber-acceptance-tests.md`). They are checked by hand: the services, the simulator and the
partner service mock all run, the flight planner and the load planner use the pages, and the partner service
shows what the partners receive.

## In short

- The partner service (`ksmeyers/partner-service`, from `docker-compose.override.yml`) reads
  `partner-exchange` through its own queue `partner-inbox` and lists every message on http://localhost:8000.
- Use a coming date: Flight Operations removes flights older than 7 days, so the dates of the
  feature files (9 January 2026) cannot be used. Only the day in the telegram changes.
- Start Load Planning with a longer loading period, so a load can be finalized a day ahead.
- The counters "with remarks" and "duplicates" of the partner service must stay 0.

## Starting

1. `docker compose up -d`: Postgres, RabbitMQ, Keycloak and the partner service.
2. Start Messaging first (it declares the queues), then Flight Operations, Load Planning and the simulator.
3. Start Load Planning with a loading period that opens 48 h before the STD. The default of 12 h only
   lets you finalize within 12 h before the STD. See `policy-values.md`.

   ```bash
   ./gradlew :load-planning:load-planning-service:bootRun --args='--load-planning.policy.loading-opens-before-departure=48h'
   ```

   On Windows (cmd), use double quotes: `--args="--load-planning.policy.loading-opens-before-departure=48h"`.
4. Log in on http://localhost:8080 as `flight_planner` and on http://localhost:8081 as `load_planner`.
5. In the partner service, click "Clear view" before you start.

## Test data

Send the two flights of the feature files with the simulator, here for Thursday 29 October 2026:

```http
POST http://localhost:8090/api/flight-schedules
Content-Type: application/json

[
  {"periodFrom":"2026-10-29","periodTo":"2026-10-29","weekdays":["THURSDAY"],
   "departureTime":"06:30","arrivalTime":"09:45","flightNumber":"HJ221",
   "departureAirport":"BRU","arrivalAirport":"ATH","registration":"SXBAA"},
  {"periodFrom":"2026-10-29","periodTo":"2026-10-29","weekdays":["THURSDAY"],
   "departureTime":"22:30","arrivalTime":"01:45","flightNumber":"3V11",
   "departureAirport":"BRU","arrivalAirport":"ATH","registration":"OOTHA"}
]
```

- A telegram then says `HJ221/29.SXBAA.BRU` instead of `HJ221/09.SXBAA.BRU`; the rest is as in the feature file.
- The pages show Brussels time and the telegrams UTC. After 25 October Brussels is UTC+1: STD 06:30 UTC
  shows as 07:30.
- Delays add up on a flight. Remove a delay before the next scenario, or announce one flight per scenario.

## Doing the steps

| Step in a scenario | How |
| --- | --- |
| A delay is entered, changed or removed | Flight details in Flight Operations |
| The finalized load has been received | Finalize Load in Load Planning; wait for the CPM in the partner service |
| The flight has departed | Finalized load, then register the ATD with the current time. The ATD form fills in the scheduled departure date: change it to today, or the ATD is "in the future" |
| The flight has landed | Register the ATA with today's date, later than the ATD |
| Something is sent twice | REST API with the `http/*.http` file of the service, environment `dev`: run the login request first. The pages hide a form once it has been used |
| No message is sent | Wait a few seconds (the relays run every second) and check that the counter does not change |

## Results of 28 October 2026

| Story | Scenario | Seen in the partner service | Result |
| --- | --- | --- | --- |
| #1, #3, #31 | One FLM per new flight | 2 FLMs; 3V11 has STA `2026-10-30T01:45Z`, departure date stays 29 | ✓ |
| #27 | Identification, moment and format version | In the body (FLM, CPM) and in the AMQP headers and properties (all) | ✓ |
| #11 | A changed ETD | `ED0645` + `EA1000` | ✓ |
| #11 | A changed departure delay (15 → 30) | `ED0700` + `EA1015` | ✓ |
| #11 | Only the description changes | No message | ✓ |
| #11 | Removing the last delay | `ED0630` + `EA0945` | ✓ |
| #11 | Only a changed ETA (10 min) | Only `EA0955` | ✓ |
| #11 | Removing an arrival delay | Only `EA0945` | ✓ |
| #25 | A load without cargo | CPM with `"ulds": []` | ✓ |
| #25 | The CPM releases the departure | Flight Operations accepts the ATD after the CPM | ✓ |
| #14 | A registered departure | `AD1056` | ✓ |
| #11 | An arrival delay after departure (20 min) | Only `EA1005` | ✓ |
| #11 | A delay across midnight (3V11, 120 min) | `3V11/29.OOTHA.BRU` `ED0030` + `…ATH` `EA0345` | ✓ |
| #11 | No delay reason in a telegram | Every body has only the three telegram lines | ✓ |
| #14 | A registered arrival | `AA1100` (HJ221), `AA1101` (3V11) | ✓ |
| #12 | The same ATD twice | Second ATD through the API: 409 "already been registered"; one MVT-AD | ✓ |

All 21 messages: 0 with remarks, 0 duplicates.

The second round used new flights on 30 October (HJ300 from BRU and from ATH, HJ301) and one of the same
day (HJ400).

`load-planning/load-planning-service/http/e2e-hand-checks.http` has the requests and the expected results of
both rounds, in order: block 1 is the first round, blocks 2A to 2E the second.

| Story | Scenario | Seen | Result |
| --- | --- | --- | --- |
| #2 | Receiving the same SSM twice | No second FLM, no second cargo in Load Planning | ✓ |
| #2, #1 | An existing flight is skipped, different data does not overwrite | Only 2 November created (FLM STD `07:00Z`); 30 October keeps 06:30 UTC and SXBAA (telegrams `HJ300/30.SXBAA.BRU`) | ✓ |
| #2, #31 | Same number from another airport; two records give two SSMs | 2 `messageIds`, FLMs for HJ300 ATH and HJ301 | ✓ |
| #32, #15 | Three booking records | 3 `messageIds`; the ULDs are on HJ300 and HJ301 without a load position | ✓ |
| #18 | Assigning a load position | AKE16689R7 on CL; no message to the partners | ✓ |
| #26 | After departure, Load Planning refuses new bookings | Load Planning log "rejected and set aside on load-planning.dlq: ... the flight has departed"; HJ221 and 3V11 no longer in the list | ✓ |
| #26 | A delay shifts the end of the loading period | HJ400 STD 12:35 UTC, delay of 25 minutes at 12:10; AKE44444R7 placed at 12:21 UTC, after the original end (12:15) | ✓ |
| #28 | Messaging not running | Events waited in `messaging.flights`; after the restart one `ED0815` and one `EA1130` | ✓ |
| #28 | RabbitMQ unreachable, changes in order | `ED0645`, `EA1000`, `ED0700`, `EA1015`, in this order, each once | ✓ |
| #28 | A CPM is not lost | Finalized during the outage; "stays in the outbox" until RabbitMQ was back; one CPM; the ATD afterwards was accepted (`AD1233`) | ✓ |
| #24 | Finalizing twice | Second finalize through the API: refused "The load is finalized and can no longer change"; one CPM | ✓ |

All 15 messages of the second round: 0 with remarks, 0 duplicates.

## The `@ui` scenarios, 4 November 2026

Checked in Edge on `main` with every open merge request of sprint 3 merged in, as `flight_planner` and as
`load_planner`. A small script did what a planner does on the page (fill in, choose, click, read the text)
and kept a screenshot of every step; a person can do the same steps by hand in this order.

Test data, sent with the simulator: HJ221 (20:00 UTC), 3V11 (22:30 UTC, lands the next day), HJ300 and HJ600
on 4 November, HJ400 on 3 November at 23:00 UTC (00:00 in Brussels on the 4th), HJ221 on 5 November, and HJ700
(15:30 UTC). Four ULDs on HJ221 (one for IST), one on HJ300 and HJ700. Load Planning ran with
`--load-planning.policy.loading-closes-before-departure=1m`, so HJ700 could be finalized and then depart within a
few minutes; with the default of 20 minutes the steps are the same, only later.

| Story | Scenario | Seen | Result |
| --- | --- | --- | --- |
| #4 | Today's flights by default | The five flights of 4 November, not HJ400 (3 November) or HJ221 of the 5th | ✓ |
| #4 | Choosing another day | Previous day: only HJ400 | ✓ |
| #4 | A day without flights | "Geen vluchten op 12/11/2026." | ✓ |
| #4 | Clicking through to the details | The flight number opens the details of that flight | ✓ |
| #4, #6 | The day of a flight is its scheduled departure date in UTC | 3V11 (23:30 in Brussels) on 4 November; HJ400 (00:00 in Brussels on the 4th) on the chart of the 3rd, after the dotted line of midnight in Brussels | ✓ |
| #5 | No estimated times without a delay | BRU – Brussels Airport, ATH – Athens International Airport, STD and STA with date, no ETD or ETA | ✓ |
| #5 | With a departure delay | ETD 21:25 and ETA 00:40 with date; the delay with 25 min, `91 – Load connection` and the description | ✓ |
| #5 | With only an arrival delay only the ETA is shown | 3V11 with 15 min arrival delay: ETA 03:00, no ETD | ✓ |
| #5 | Departure and arrival delays can be told apart | Column "Voor": Vertrek, Aankomst | ✓ |
| #5 | After the actual times are registered | ATD 16:36 and ATA 16:37 with date | ✓ |
| #5 | Scheduled and estimated times cannot be edited directly | No input on STD, STA, ETD or ETA | ✓ |
| #6 | Flights as bars on a timeline | One bar per flight, from STD to STA | ✓ |
| #6 | Choosing another day | Previous day: only HJ400 | ✓ |
| #6 | A flight across midnight runs to the edge | 3V11 runs to the right edge; the row says it lands 05/11 02:45 | ✓ |
| #6 | A flight of the day before that is still in the air continues | HJ400 starts at the left edge; "5 vluchten, en 1 van de vorige dag nog onderweg" | ✓ |
| #6 | The timeline is the whole UTC day in Brussels time | Hours 01 … 23, 05/11 | ✓ |
| #7 | Times are shown in Brussels time | STD 20:00 UTC shown as 21:00 CET | ✓ (winter time; the July row: see below) |
| #7 | Every screen states which time it shows | Overview, details, Gantt chart, flights to load and cargo page | ✓ |
| #7 | Entered times are stored in UTC | ATD 16:36 Brussels gives `HJ700/04 AD 15:36` at the partner service | ✓ (winter time) |
| #7 | A local time that does not exist is refused | 29/03/2026 02:30: "02:30 bestaat niet in Brussel op 29/03/2026: de klok springt dan van 02:00 naar 03:00." | ✓ |
| #7 | For an ambiguous local time the planner chooses | 25/10/2026 02:30: the page asks "02:30 zomertijd (00:30 UTC)" or "02:30 wintertijd (01:30 UTC)" and sends nothing before the choice | ✓ |
| #9, #10 | Extra, no `@ui` scenario: adding, changing and removing a delay | 25 → 30 min moves the ETD to 21:30; removing asks "De aankomstvertraging van 15 min verwijderen?" and moves the ETA back | ✓ |
| #12, #13 | Extra, no `@ui` scenario: registering the ATD and the ATA | Status Opgestegen, then Geland; "De aankomst is geregistreerd." | ✓ |
| #16 | Today's flights by default, without departed or landed flights | HJ700 (landed) not in the list | ✓ |
| #16 | Turning the filter off | HJ700 appears, with "Opgestegen" and "Gefinaliseerd" | ✓ |
| #16 | Choosing another day | Next day: only HJ221 of 5 November | ✓ |
| #16 | Clicking through to the cargo page | The flight number opens its cargo page | ✓ |
| #17 | All booked ULDs, with and without a load position | Code, weight with unit (2646 lb shown as booked), destination | ✓ |
| #17 | The selection list contains exactly the load positions of the floor plan | A1, A2, B1, CL … UR, X: 34 positions | ✓ |
| #17 | A finalized load can be recognized | "De lading is gefinaliseerd door Lars Loadplanner: laadposities kunnen niet meer wijzigen." and no lists | ✓ |
| #18 | Extra, no `@ui` scenario: assigning and clearing with the list | AKE11111R7 on CR; "Leegmaken" in the list clears; a position held by another ULD shows it and cannot be chosen | ✓ |
| #18 | The deck shows free and taken positions | Free positions "vrij", taken ones with their ULD | ✓ |
| #18 | Placing a ULD by clicking a free position | PMC22222KL picked, then DL clicked | ✓ |
| #18 | The deck shows which ULD is in the way | AKE44444R7 on A1 ✕, AKE33333R7 (IST) on CL ▲ | ✓ |
| #18 | The deck shows the weight on each side | "Links 2180.2 kg · Rechts 1200 kg · de zwaarste kant mag hoogstens 1320 kg dragen" | ✓ |
| #18 | A flight planner sees the deck without changing it | 34 positions, none clickable | ✓ |
| #18 | Extra (the `@integration` scenario in two browser tabs): two load planners choose the same position at the same moment | Two tabs choose A2 at once: one gets it, the other "Someone else changed the load of this flight at the same time" and then sees the winner on A2 | ✓ |
| #23 | One report per check | Lateral balance and Offloading failed (hard) with their reason, Not all cargo loaded passed (soft) | ✓ |
| #23 | Not outside the loading period | HJ600: "De beladingsperiode is afgelopen sinds …", no Check Load | ✓ |
| #24 | Extra, no `@ui` scenario: Finalize Load only when all hard checks pass | Disabled with "Een hard check faalt: zie het rapport." | ✓ |
| #24 | Finalizing shows the same report as Check Load | After a passing check another planner put a ULD on CL; Finalize Load: "De lading is niet gefinaliseerd: een hard check faalt." with the same report as Check Load | ✓ |
| #24 | Extra, no `@ui` scenario: finalizing | A confirmation, then the finalized load (HJ300, HJ700). Done on `main` just before the merge requests were added; they do not change it | ✓ |
| #34 | A load planner sees no actions for flights | "Alleen een flight planner kan vertragingen, het vertrek en de aankomst ingeven." | ✓ |
| #34 | A flight planner sees no actions for the load | "Alleen een load planner kan laadposities wijzigen." | ✓ |
| #34 | Actions follow the state of the flight | Departed: only an arrival delay and the ATA; landed: no forms | ✓ |
| #34 | Actions follow the loading period | HJ600 after its loading period: no lists, no Check Load or Finalize Load | ✓ |
| #35 | Not logged in | Both contexts send the browser to the login of Keycloak | ✓ |
| #36 | The screen shows the reason for a refusal | ATD 16:36 Brussels on HJ700 (ETD 15:30 UTC): "The ATD is more than 5 minutes after the ETD: enter a departure delay first", the flight stays planned | ✓ |
| #44 | The details page and the cargo page show who changed the flight or the load | "Fien Flightplanner" next to the delay and the ATD and ATA, "Lars Loadplanner" next to the ULD and the finalized load | ✓ |

No console errors on any page. All messages of this round at the partner service: 0 with remarks, 0
duplicates.

Not checked in the browser:

| Scenario | Why | Covered by |
| --- | --- | --- |
| #7 the July rows (summer time) of "Times are shown in Brussels time" and "Entered times are stored in UTC" | A flight in July is older than the 7 days the retention keeps, and an ATD cannot be in the future | `presenter/brusselsTime.test.ts` (UTC shown as CET or CEST) |
| #18 a keyboard choice in an open list on macOS | No Mac | – |

## Not checked by hand

| Scenario | Why | Covered by |
| --- | --- | --- |
| #14 departure or arrival after midnight | The ATD and ATA cannot be in the future, so by day they are never after midnight | `@unit` scenario of #14 "An ATD and ATA after midnight keep the day of the scheduled departure date" |
| #12 the same ATD twice "in quick succession" | The HTTP client sends the requests one after the other, not at the same moment | `@unit` scenario of #12 "A registered ATD cannot be changed" |
| #38 removing old flights | The job runs at 00:00 UTC | `@integration` scenarios of #38 in all three services: Flight Operations removes the flight, Load Planning its cargo, Messaging its messages |
| #28 an FLM is not lost | RabbitMQ would have to go down in the second between creating the flight and sending its event | The same outbox relay as the delays and the CPM, checked above |

## References

- `cucumber-acceptance-tests.md`: the tags `@unit`, `@integration` and `@e2e`.
- `policy-values.md`: changing a policy value without a new build.
- `README.md`, "Berichten tussen de services": the exchanges and queues.
