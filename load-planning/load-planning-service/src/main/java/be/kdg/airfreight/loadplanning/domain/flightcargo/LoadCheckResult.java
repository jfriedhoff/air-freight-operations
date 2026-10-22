package be.kdg.airfreight.loadplanning.domain.flightcargo;

import java.util.Objects;

import org.jmolecules.ddd.annotation.ValueObject;
import org.jspecify.annotations.Nullable;

/** One check of the report: passed or failed, why it failed, and what it found as data, if the check has any. */
@ValueObject
public record LoadCheckResult(String checkName, CheckSeverity severity, boolean passed, @Nullable String reason,
                              @Nullable LoadCheckDetails details) {
    public LoadCheckResult {
        Objects.requireNonNull(checkName, "Check name cannot be null");
        Objects.requireNonNull(severity, "Severity cannot be null");
    }

    public static LoadCheckResult pass(String checkName, CheckSeverity severity) {
        return new LoadCheckResult(checkName, severity, true, null, null);
    }

    public static LoadCheckResult pass(String checkName, CheckSeverity severity, LoadCheckDetails details) {
        return new LoadCheckResult(checkName, severity, true, null,
            Objects.requireNonNull(details, "Details cannot be null"));
    }

    public static LoadCheckResult fail(String checkName, CheckSeverity severity, String reason) {
        return new LoadCheckResult(checkName, severity, false, Objects.requireNonNull(reason, "Reason cannot be null"),
            null);
    }

    public static LoadCheckResult fail(String checkName, CheckSeverity severity, String reason,
                                       LoadCheckDetails details) {
        return new LoadCheckResult(checkName, severity, false, Objects.requireNonNull(reason, "Reason cannot be null"),
            Objects.requireNonNull(details, "Details cannot be null"));
    }
}
