# Frontend tests

Both frontends have unit tests for their logic: Brussels time, the place of a bar in the Gantt chart,
what a page offers to which role, and how an error from the server is worded. They run with
`node --test`, the test runner that is built into Node, so the project needs no extra test library.

## In short

- A test file sits next to the code it tests and ends in `.test.ts`, for example
  `src/ts/presenter/brusselsTime.test.ts` next to `brusselsTime.ts`.
- Node runs the TypeScript files as they are: it removes the types and runs the rest (type stripping,
  Node 22.18 or later). Our `tsconfig.json` already allows only syntax that can be removed like that
  (`erasableSyntaxOnly`) and imports with `.ts` at the end, so the code needs no build step first.
- `./gradlew build` runs them (the `check` task depends on `npm_run_test`). In GitLab every frontend has
  its own job, `flight-operations-frontend` and `load-planning-frontend` (see [`ci-pipeline.md`](ci-pipeline.md)).
- `tsc` also type-checks the test files, so a test that no longer fits the code fails `npm run build`.

## What is tested

| Frontend | File | What it checks |
| --- | --- | --- |
| Flight Operations | `presenter/brusselsTime.test.ts` | UTC shown as Brussels time with CET or CEST, a late UTC time that is already the next day in Brussels, Brussels midnight on the days the clock changes, the hour labels of the Gantt chart (02 twice on the night the clock goes back) and its day label at midnight, and `brusselsToUtc` for a time that does not exist or exists twice (story 7) |
| Flight Operations | `presenter/ganttTimeline.test.ts` | Where a bar starts and how wide it is, an arrival after the end of the day cut off at the edge, the minimum width of a bar, the place of the Brussels midnight line in winter and in summer |
| Flight Operations | `domain/Flight.test.ts` | Which delays the page offers for each status and role, and when an ETD or ETA is shown |
| Load Planning | `domain/FlightCargo.test.ts` | Which words the page chooses when the load cannot change, including the edges of the loading period; who may change the load; the ULDs without a load position |
| Load Planning | `domain/Deck.test.ts` | The deck of the floor plan: columns and rows from the floor plan (nose and tail on the centre line, L and R of a row in one column, the door), the colour by destination, the marks of the offloading check only where the report's ULD is still drawn, a cell that is stuck and in the way, the side weights and their rounding, the targets of a picked ULD, and when a pick survives a reload |
| Load Planning | `domain/LoadCheckReport.test.ts` | Finalize Load can be used when all hard checks pass (a failed soft check does not stop it), not while a hard check fails, and without a report of the checks the server decides (story 24) |
| Load Planning | `domain/FlightToLoad.test.ts` | The address of the list with and without departed flights, and when an ETD or ETA is shown |
| Load Planning | `presenter/brusselsTime.test.ts` | UTC shown as Brussels time with CET or CEST, moving a day forward or back across a month and a year, and the Dutch title of a day |
| Both | `domain/FlightIdentity.test.ts` | A page of the other context reads back the identity it was opened with; an address that misses one of the three parts has none |
| Both | `infrastructure/restUtils.test.ts` | The error message carries the reason the server gives (Problem Details), and the page's own words for 401, 403 and other answers |

These tests check what the page shows and offers. Whether an action is allowed is always decided by the
server, and the Java tests check that.

## What is not tested, and why

| Code | Why not |
| --- | --- |
| The presenters (`*Presenter.ts`), `autoRefresh.ts`, `htmlUtil.ts` | They look up elements of the page as soon as they load. Node has no `document`, so testing them would need a fake browser such as jsdom. They mostly put data on the screen, and the `@ui` scenarios are checked by hand. |
| `auth.ts` | It starts Keycloak as soon as it loads. |
| `contexts.ts` | It reads the address of the other context from the Vite build (`import.meta.env`), which only exists when Vite builds the page. |

The rule for new code: put logic that is worth a test in a file that does not touch the page, as
`ganttTimeline.ts` does for the Gantt chart. The presenter then only puts the result on the screen.

## Running it

- Terminal, in `flight-operations/flight-operations-frontend` or `load-planning/load-planning-frontend`:
  `npm test`.
- Gradle: `./gradlew :flight-operations:flight-operations-frontend:npm_run_test` (or
  `:load-planning:load-planning-frontend:npm_run_test`), or `./gradlew build` for everything.
- In GitLab the results are in the Tests tab of the pipeline. The job writes them as a JUnit file
  (`--test-reporter=junit`).

## Writing a test

```ts
import {test} from "node:test";
import assert from "node:assert/strict";
import {listQuery} from "./FlightToLoad.ts";

test("the list hides departed flights unless the address asks for them", () => {
    assert.equal(listQuery("2026-01-09", false), "?scheduledDepartureDate=2026-01-09");
});
```

- Name the test after the rule, in a sentence, as with the Cucumber scenarios.
- `assert.deepEqual` compares objects and arrays, also `Date` values. `assert.rejects` checks an
  `async` function that throws.
- Write times as UTC with a `Z` at the end (`"2026-01-09T06:30:00Z"`), as the API sends them. The
  Brussels time is then the same on every machine, whatever its time zone is.

## Why `node --test` and not Vitest

Vitest is the usual test library for a Vite project and would work as well. `node --test` was chosen
because it comes with Node, which the build already needs: there is nothing to install, the CI job
needs no `npm install`, and there is one tool less to explain. If the presenters get tests some day,
Vitest with jsdom is the step to take.

## References

- [Node.js — Test runner](https://nodejs.org/api/test.html): `test`, the reporters, running files by
  pattern.
- [Node.js — Assert](https://nodejs.org/api/assert.html): `assert.equal`, `deepEqual`, `rejects`.
- [Node.js — Modules: TypeScript](https://nodejs.org/api/typescript.html): type stripping, and why
  only syntax that can be removed is allowed.
- [TypeScript — `erasableSyntaxOnly`](https://www.typescriptlang.org/tsconfig/#erasableSyntaxOnly):
  the compiler setting that matches it.
- [Vitest](https://vitest.dev/): the alternative.
