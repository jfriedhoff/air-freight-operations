export type CheckSeverity = "HARD" | "SOFT";

/* One check of the report. A failed check carries its reason. */
export interface LoadCheckResult {
    checkName: string;
    severity: CheckSeverity;
    passed: boolean;
    reason: string | null;
}

/* The report of Check Load, and the answer of Finalize Load. */
export interface LoadCheckReport {
    hardChecksPassed: boolean;
    results: LoadCheckResult[];
}

/* Finalize Load answers with the report whether it finalized the load or not. */
export interface FinalizeOutcome {
    finalized: boolean;
    report: LoadCheckReport;
}
