package be.kdg.airfreight.messaging.domain;

import java.util.Objects;

import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
public record AircraftRegistration(String value) {
    public AircraftRegistration {
        Objects.requireNonNull(value, "Registration cannot be null");
        if (value.isBlank()) {
            throw new DomainRuleViolationException("The aircraft registration cannot be blank");
        }
    }
}
