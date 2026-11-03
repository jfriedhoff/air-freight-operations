# Hand checks for the `@e2e` scenarios

Scenarios tagged `@e2e` cross several services, so Cucumber never runs them (see
`cucumber-acceptance-tests.md`). They are checked by hand: the services, the simulator and the course's
partner service all run, the flight planner and the load planner use the pages, and the partner service
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
