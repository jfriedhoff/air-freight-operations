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
        return utcDateOf(departure);
    }

    /**
     * The day an instant falls on: its UTC date. A day starts at 00:00 UTC, also for the staff in Brussels, so the
     * overview, the retention period and the telegrams count days the same way.
     */
    public static LocalDate utcDateOf(Instant instant) {
        return LocalDate.ofInstant(instant, ZoneOffset.UTC);
    }
}
