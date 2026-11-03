export type CheckSeverity = "HARD" | "SOFT";

/* A ULD and the load position it stands on. */
export interface UldOnDeck {
    uld: string;
    position: string;
}

/* A ULD for the arrival airport that cannot be offloaded, and the ULDs in its way. */
export interface Obstruction {
    stuck: UldOnDeck;
    inTheWay: UldOnDeck[];
}

export interface Offloading {
    obstructions: Obstruction[];
}

/* The weight on each side and the most the heaviest side may carry, in kg, as the check computed them. */
export interface LateralBalance {
    leftKilograms: number;
    rightKilograms: number;
    maximumOnTheHeaviestSide: number;
    heavierSide: "LEFT" | "RIGHT" | null;
}

/* One check of the report. A failed check carries its reason; the two hard checks also what they found. */
export interface LoadCheckResult {
    checkName: string;
    severity: CheckSeverity;
    passed: boolean;
    reason: string | null;
    offloading: Offloading | null;
    lateralBalance: LateralBalance | null;
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

/* Finalize Load can be used as soon as all hard checks pass, as the case says. The page asks the checks after every
   load, so the report is the one of the load as it stands. Without a report (the checks could not be asked) the button
   stays usable: the server runs the checks again when finalizing and refuses when a hard check fails. */
export function canFinalize(report: LoadCheckReport | null): boolean {
    return report === null || report.hardChecksPassed;
}
