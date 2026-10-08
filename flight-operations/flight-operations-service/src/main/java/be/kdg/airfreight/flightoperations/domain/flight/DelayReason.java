package be.kdg.airfreight.flightoperations.domain.flight;

import java.util.Objects;

import be.kdg.airfreight.flightoperations.domain.InvalidValueException;
import org.jmolecules.ddd.annotation.ValueObject;

/** One line of the IATA delay-code list: the code and what it stands for. */
@ValueObject
public record DelayReason(DelayCode code, String description) {
    public DelayReason {
        Objects.requireNonNull(code, "Code cannot be null");
        Objects.requireNonNull(description, "Description cannot be null");
        if (description.isBlank()) {
            throw new InvalidValueException("The description of a delay reason cannot be blank");
        }
    }
}
