import {test} from "node:test";
import assert from "node:assert/strict";
import {addDays, brusselsDateTime, brusselsTime, longDate, plainDate} from "./brusselsTime.ts";

test("a time is shown with its date and the Brussels zone", () => {
    assert.equal(brusselsDateTime("2026-01-09T06:30:00Z"), "09/01/2026 07:30 CET");
    assert.equal(brusselsDateTime("2026-07-09T06:30:00Z"), "09/07/2026 08:30 CEST");
});

test("a late UTC time is already the next day in Brussels", () => {
    assert.equal(brusselsDateTime("2026-01-09T23:30:00Z"), "10/01/2026 00:30 CET");
});

test("the time of the last update has no date", () => {
    assert.equal(brusselsTime(new Date("2026-07-09T06:31:00Z")), "08:31");
});

test("a date without a time is not converted", () => {
    assert.equal(plainDate("2026-01-09"), "09/01/2026");
});

test("adding days crosses months and years", () => {
    assert.equal(addDays("2026-12-31", 1), "2027-01-01");
    assert.equal(addDays("2026-03-01", -1), "2026-02-28");
});

test("the title of a day names the weekday in Dutch", () => {
    assert.equal(longDate("2026-01-09"), "vrijdag 9 januari 2026");
});
