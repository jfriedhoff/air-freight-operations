package be.kdg.airfreight.loadplanning.domain.flightcargo;

import java.util.List;
import java.util.Objects;

import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
public record LoadCheckReport(List<LoadCheckResult> results) {
    public LoadCheckReport {
        Objects.requireNonNull(results, "Results cannot be null");
        results = List.copyOf(results);
    }

    public boolean hardChecksPassed() {
        return results.stream()
            .filter(result -> result.severity() == CheckSeverity.HARD)
            .allMatch(LoadCheckResult::passed);
    }
}
