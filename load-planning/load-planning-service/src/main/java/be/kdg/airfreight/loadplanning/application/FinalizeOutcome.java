package be.kdg.airfreight.loadplanning.application;

import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadCheckReport;

/**
 * What Finalize Load did: whether the load is finalized now, and the report of the checks that decided it. The
 * use case says it itself, so a caller never has to work it out from the report.
 */
public record FinalizeOutcome(boolean finalized, LoadCheckReport report) {
}
