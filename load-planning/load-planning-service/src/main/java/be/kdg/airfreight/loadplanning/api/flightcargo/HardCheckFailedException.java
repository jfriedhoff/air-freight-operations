package be.kdg.airfreight.loadplanning.api.flightcargo;

/**
 * Finalize Load was refused because a hard check fails. Unlike the other refusals it carries the report, so the
 * answer tells the load planner what to change. Nothing changed: the use case only finalizes when every hard
 * check passes.
 */
public class HardCheckFailedException extends RuntimeException {
    private final LoadCheckReportDto report;

    HardCheckFailedException(LoadCheckReportDto report) {
        super("The load cannot be finalized: a hard load check fails");
        this.report = report;
    }

    public LoadCheckReportDto getReport() {
        return report;
    }
}
