package be.kdg.airfreight.flightoperations.domain.flight;

import java.time.Instant;
import java.util.Objects;

import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
public record EstimatedTimes(Instant departure, Instant arrival) {
    public EstimatedTimes {
        Objects.requireNonNull(departure, "ETD cannot be null");
        Objects.requireNonNull(arrival, "ETA cannot be null");
    }
}
