package be.kdg.airfreight.flightoperations.domain.flight;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import be.kdg.airfreight.flightoperations.domain.DomainRuleViolationException;
import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
public record ActualTimeTolerance(Duration value) {
    public ActualTimeTolerance {
        Objects.requireNonNull(value, "Tolerance cannot be null");
        if (value.isNegative()) {
            throw new DomainRuleViolationException("The tolerance cannot be negative");
        }
    }

    /** An actual time is allowed up to the estimated time plus the tolerance, that moment included. */
    public boolean allows(Instant actual, Instant estimated) {
        return !actual.isAfter(estimated.plus(value));
    }
}
