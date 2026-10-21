import {test} from "node:test";
import assert from "node:assert/strict";
import {brusselsToUtc} from "./brusselsTime.ts";

test("in winter Brussels is one hour ahead of UTC", () => {
    assert.deepEqual(brusselsToUtc("2026-01-09", "07:50"), {kind: "exact", utc: new Date("2026-01-09T06:50:00Z")});
});

test("in summer Brussels is two hours ahead of UTC", () => {
    assert.deepEqual(brusselsToUtc("2026-07-09", "08:50"), {kind: "exact", utc: new Date("2026-07-09T06:50:00Z")});
});

test("a time skipped when the clock goes forward does not exist", () => {
    assert.deepEqual(brusselsToUtc("2026-03-29", "02:30"), {kind: "nonexistent"});
});

test("the minutes around the skipped hour do exist", () => {
    assert.deepEqual(brusselsToUtc("2026-03-29", "01:59"), {kind: "exact", utc: new Date("2026-03-29T00:59:00Z")});
    assert.deepEqual(brusselsToUtc("2026-03-29", "03:00"), {kind: "exact", utc: new Date("2026-03-29T01:00:00Z")});
});

test("a time repeated when the clock goes back has a summer and a winter reading", () => {
    assert.deepEqual(brusselsToUtc("2026-10-25", "02:30"), {
        kind: "ambiguous",
        summer: new Date("2026-10-25T00:30:00Z"),
        winter: new Date("2026-10-25T01:30:00Z"),
    });
});

test("the minutes around the repeated hour have one reading", () => {
    assert.deepEqual(brusselsToUtc("2026-10-25", "01:59"), {kind: "exact", utc: new Date("2026-10-24T23:59:00Z")});
    assert.deepEqual(brusselsToUtc("2026-10-25", "03:00"), {kind: "exact", utc: new Date("2026-10-25T02:00:00Z")});
});
