package be.kdg.airfreight.flightoperations.domain.flight;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Objects;

import be.kdg.airfreight.flightoperations.domain.InvalidValueException;
import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
public record ScheduledTimes(Instant departure, Instant arrival) {
    public ScheduledTimes {
        Objects.requireNonNull(departure, "STD cannot be null");
        Objects.requireNonNull(arrival, "STA cannot be null");
        if (!arrival.isAfter(departure)) {
            throw new InvalidValueException("The STA must be later than the STD");
        }
    }

    /** The scheduled departure date: the UTC date of the STD. It never changes. */
    public LocalDate departureDate() {
        return LocalDate.ofInstant(departure, ZoneOffset.UTC);
    }
}
