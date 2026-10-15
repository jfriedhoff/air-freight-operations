package be.kdg.airfreight.loadplanning.api.flightcargo;

import java.util.List;

import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadCheckReport;

/**
 * The report of Check Load. Finalize Load answers with the same report, whether it finalized the load or not,
 * so the page shows one kind of report for both buttons. One result per check, in the order the checks ran.
 */
public record LoadCheckReportDto(boolean hardChecksPassed, List<LoadCheckResultDto> results) {
    public static LoadCheckReportDto from(LoadCheckReport report) {
        return new LoadCheckReportDto(report.hardChecksPassed(),
            report.results().stream().map(LoadCheckResultDto::from).toList());
    }
}
