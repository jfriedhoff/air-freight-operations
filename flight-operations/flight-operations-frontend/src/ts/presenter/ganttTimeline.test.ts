import {test} from "node:test";
import assert from "node:assert/strict";
import {barPlace, brusselsMidnightPercent, hoursOf, timelineOf} from "./ganttTimeline.ts";

const DAY = timelineOf("2026-01-09");

function assertNear(actual: number, expected: number): void {
    assert.ok(Math.abs(actual - expected) < 1e-9, `${actual} is not ${expected}`);
}

test("the timeline is the UTC day", () => {
    assert.equal(DAY.start, Date.parse("2026-01-09T00:00:00Z"));
    assert.equal(DAY.end, Date.parse("2026-01-10T00:00:00Z"));
});

test("a bar runs from STD to STA", () => {
    assert.deepEqual(barPlace(DAY, "2026-01-09T06:00:00Z", "2026-01-09T12:00:00Z"),
        {leftPercent: 25, widthPercent: 25, cutOff: false});
});

test("an arrival after the end of the day is cut off at the edge", () => {
    const bar = barPlace(DAY, "2026-01-09T22:00:00Z", "2026-01-10T02:00:00Z");
    assertNear(bar.leftPercent, 22 / 24 * 100);
    assertNear(bar.widthPercent, 2 / 24 * 100);
    assert.equal(bar.cutOff, true);
});

test("a departure at 23:59 UTC still gets a visible bar inside the chart", () => {
    const bar = barPlace(DAY, "2026-01-09T23:59:00Z", "2026-01-10T03:00:00Z");
    assert.equal(bar.widthPercent, 0.4);
    assertNear(bar.leftPercent, 99.6);
});

test("the timeline has 24 hours", () => {
    const hours = hoursOf(DAY);
    assert.equal(hours.length, 24);
    assert.deepEqual(hours[0], {moment: new Date("2026-01-09T00:00:00Z"), percent: 0});
    assertNear(hours[23].percent, 23 / 24 * 100);
});

test("Brussels midnight lies one hour before the end of the UTC day in winter, two in summer", () => {
    assertNear(brusselsMidnightPercent(DAY, "2026-01-09"), 23 / 24 * 100);
    assertNear(brusselsMidnightPercent(timelineOf("2026-07-09"), "2026-07-09"), 22 / 24 * 100);
});
