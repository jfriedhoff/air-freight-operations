package be.kdg.airfreight.loadplanning.domain.flightcargo;

import java.time.Instant;
import java.util.Objects;

import org.jmolecules.ddd.annotation.ValueObject;

/** The period in which the load may change: the start is included, the end is not. */
@ValueObject
public record LoadingPeriod(Instant start, Instant end) {
    public LoadingPeriod {
        Objects.requireNonNull(start, "Start cannot be null");
        Objects.requireNonNull(end, "End cannot be null");
    }

    public boolean contains(Instant now) {
        return !now.isBefore(start) && now.isBefore(end);
    }
}
