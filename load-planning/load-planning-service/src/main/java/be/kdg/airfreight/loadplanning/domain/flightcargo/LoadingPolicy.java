package be.kdg.airfreight.loadplanning.domain.flightcargo;

import java.time.Duration;
import java.util.Objects;

import be.kdg.airfreight.loadplanning.domain.InvalidValueException;
import org.jmolecules.ddd.annotation.ValueObject;

/** The configurable 12 hours and 20 minutes of the loading period. */
@ValueObject
public record LoadingPolicy(Duration opensBeforeScheduledDeparture, Duration closesBeforeEstimatedDeparture) {
    public LoadingPolicy {
        Objects.requireNonNull(opensBeforeScheduledDeparture, "Opening time cannot be null");
        Objects.requireNonNull(closesBeforeEstimatedDeparture, "Closing time cannot be null");
        if (opensBeforeScheduledDeparture.isNegative() || closesBeforeEstimatedDeparture.isNegative()) {
            throw new InvalidValueException("The times of the loading period cannot be negative");
        }
    }
}
