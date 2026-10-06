package be.kdg.airfreight.flightoperations.domain.flight;

import be.kdg.airfreight.flightoperations.domain.DomainRuleViolationException;
import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
public record DelayDuration(int minutes) {
    public static final int MAX_MINUTES = 12 * 60;

    public DelayDuration {
        if (minutes < 1 || minutes > MAX_MINUTES) {
            throw new DomainRuleViolationException("A delay lasts at least 1 minute and at most 12 hours");
        }
    }
}
