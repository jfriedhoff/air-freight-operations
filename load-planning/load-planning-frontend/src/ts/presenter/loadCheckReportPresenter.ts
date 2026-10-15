import type {LoadCheckReport, LoadCheckResult} from "../domain/LoadCheckReport.ts";
import {createElementWithText} from "./htmlUtil.ts";

const reportSection = document.getElementById("load-check-report")!;
const reportRows = document.getElementById("load-check-results")!;

/* A failed hard check stops the flight, a failed soft check only warns: they get different colours. */
function resultRow(result: LoadCheckResult): HTMLTableRowElement {
    const row = document.createElement("tr");
    if (!result.passed) {
        row.className = result.severity === "HARD" ? "bg-error/20" : "bg-warning/20";
    }
    row.append(
        createElementWithText("td", result.checkName),
        createElementWithText("td", result.passed ? "geslaagd" : "gefaald"),
        createElementWithText("td", result.reason ?? ""),
        createElementWithText("td", result.severity === "HARD" ? "hard" : "soft"));
    return row;
}

/* The report as the server gave it, one row per check. It stays until the next report. */
export function showReport(report: LoadCheckReport): void {
    reportRows.replaceChildren(...report.results.map(resultRow));
    reportSection.classList.remove("hidden");
}
