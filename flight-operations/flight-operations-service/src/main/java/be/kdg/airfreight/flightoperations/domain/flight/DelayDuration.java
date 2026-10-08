package be.kdg.airfreight.flightoperations.domain.flight;

import be.kdg.airfreight.flightoperations.domain.InvalidValueException;
import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
public record DelayDuration(int minutes) {
    public static final int MAX_MINUTES = 12 * 60;

    public DelayDuration {
        if (minutes < 1 || minutes > MAX_MINUTES) {
            throw new InvalidValueException("A delay lasts at least 1 minute and at most 12 hours");
        }
    }
}
