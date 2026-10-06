package be.kdg.airfreight.loadplanning.domain.flightcargo;

import java.util.Objects;

import be.kdg.airfreight.loadplanning.domain.DomainRuleViolationException;
import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
public record AircraftType(String code) {
    public AircraftType {
        Objects.requireNonNull(code, "Aircraft type cannot be null");
        if (code.isBlank()) {
            throw new DomainRuleViolationException("The aircraft type cannot be blank");
        }
    }
}
