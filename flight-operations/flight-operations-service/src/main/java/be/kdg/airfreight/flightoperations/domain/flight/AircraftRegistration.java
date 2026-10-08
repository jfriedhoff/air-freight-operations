package be.kdg.airfreight.flightoperations.domain.flight;

import java.util.Objects;

import be.kdg.airfreight.flightoperations.domain.InvalidValueException;
import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
public record AircraftRegistration(String value) {
    public AircraftRegistration {
        Objects.requireNonNull(value, "Registration cannot be null");
        if (value.isBlank()) {
            throw new InvalidValueException("The aircraft registration cannot be blank");
        }
    }
}
