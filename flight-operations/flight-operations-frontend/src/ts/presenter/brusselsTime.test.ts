import {test} from "node:test";
import assert from "node:assert/strict";
import {
    addDays, brusselsDate, brusselsDateTime, brusselsDayMonth, brusselsHour, brusselsMidnight, brusselsShortDateTime,
    brusselsTimeWithZone, brusselsToUtc, longDate, plainDate,
} from "./brusselsTime.ts";

test("the hours on the timeline are Brussels hours, and on the night the clock goes back 02 appears twice", () => {
    assert.equal(brusselsHour(new Date("2026-01-09T06:00:00Z")), "07");
    assert.equal(brusselsHour(new Date("2026-10-25T00:00:00Z")), "02");
    assert.equal(brusselsHour(new Date("2026-10-25T01:00:00Z")), "02");
});

test("midnight on the timeline shows the Brussels day and month", () => {
    assert.equal(brusselsDayMonth(new Date("2026-01-09T23:00:00Z")), "10/01");
});

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

test("a time is shown with its date and the Brussels zone", () => {
    assert.equal(brusselsDateTime("2026-01-09T06:30:00Z"), "09/01/2026 07:30 CET");
    assert.equal(brusselsDateTime("2026-07-09T06:30:00Z"), "09/07/2026 08:30 CEST");
});

test("a late UTC time is already the next day in Brussels", () => {
    assert.equal(brusselsDateTime("2026-01-09T23:30:00Z"), "10/01/2026 00:30 CET");
    assert.equal(brusselsShortDateTime("2026-01-09T23:30:00Z"), "10/01 00:30 CET");
    assert.equal(brusselsDate("2026-01-09T23:30:00Z"), "2026-01-10");
});

test("the repeated hour is told apart by its zone", () => {
    assert.equal(brusselsTimeWithZone("2026-10-25T00:30:00Z"), "02:30 CEST");
    assert.equal(brusselsTimeWithZone("2026-10-25T01:30:00Z"), "02:30 CET");
});

test("Brussels midnight is 23:00 UTC in winter and 22:00 UTC in summer", () => {
    assert.deepEqual(brusselsMidnight("2026-01-10"), new Date("2026-01-09T23:00:00Z"));
    assert.deepEqual(brusselsMidnight("2026-07-10"), new Date("2026-07-09T22:00:00Z"));
});

test("on the days the clock changes, Brussels midnight still has the offset of the night before", () => {
    assert.deepEqual(brusselsMidnight("2026-03-29"), new Date("2026-03-28T23:00:00Z"));
    assert.deepEqual(brusselsMidnight("2026-10-25"), new Date("2026-10-24T22:00:00Z"));
});

test("adding days crosses months and years", () => {
    assert.equal(addDays("2026-12-31", 1), "2027-01-01");
    assert.equal(addDays("2026-03-01", -1), "2026-02-28");
});

test("a date without a time is not converted", () => {
    assert.equal(plainDate("2026-01-09"), "09/01/2026");
    assert.equal(longDate("2026-01-09"), "vrijdag 9 januari 2026");
});
