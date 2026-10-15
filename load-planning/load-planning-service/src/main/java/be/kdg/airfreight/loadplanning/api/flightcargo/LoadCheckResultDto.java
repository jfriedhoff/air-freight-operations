package be.kdg.airfreight.loadplanning.api.flightcargo;

import be.kdg.airfreight.loadplanning.domain.flightcargo.CheckSeverity;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadCheckResult;
import org.jspecify.annotations.Nullable;

/** One check of the report. A failed check carries its reason; a passed check has none. */
public record LoadCheckResultDto(String checkName, CheckSeverity severity, boolean passed, @Nullable String reason) {
    public static LoadCheckResultDto from(LoadCheckResult result) {
        return new LoadCheckResultDto(result.checkName(), result.severity(), result.passed(), result.reason());
    }
}
