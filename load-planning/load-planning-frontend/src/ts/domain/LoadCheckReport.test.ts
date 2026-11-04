import {test} from "node:test";
import assert from "node:assert/strict";
import {canFinalize} from "./LoadCheckReport.ts";
import type {CheckSeverity, LoadCheckReport, LoadCheckResult} from "./LoadCheckReport.ts";

function result(checkName: string, severity: CheckSeverity, passed: boolean): LoadCheckResult {
    return {
        checkName, severity, passed, reason: passed ? null : `${checkName} faalt`,
        offloading: null, lateralBalance: null,
    };
}

function report(hardChecksPassed: boolean, ...results: LoadCheckResult[]): LoadCheckReport {
    return {hardChecksPassed, results};
}

test("Finalize Load can be used when all hard checks pass, also with a failed soft check", () => {
    assert.equal(canFinalize(report(true, result("Offloading", "HARD", true), result("Lateral balance", "HARD", true))),
        true);
    assert.equal(canFinalize(report(true, result("Offloading", "HARD", true), result("Not all cargo loaded", "SOFT", false))), true);
});

test("Finalize Load cannot be used while a hard check fails", () => {
    assert.equal(canFinalize(report(false, result("Offloading", "HARD", true),
        result("Lateral balance", "HARD", false))), false);
});

test("without a report of the checks the server decides when finalizing", () => {
    assert.equal(canFinalize(null), true);
});
