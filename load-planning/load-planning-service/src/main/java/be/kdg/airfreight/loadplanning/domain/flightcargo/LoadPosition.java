package be.kdg.airfreight.loadplanning.domain.flightcargo;

import java.util.Objects;

import be.kdg.airfreight.loadplanning.domain.DomainRuleViolationException;
import org.jmolecules.ddd.annotation.ValueObject;
import org.jspecify.annotations.Nullable;

/** A place on the deck. A position in the nose or the tail has no side and counts for neither. */
@ValueObject
public record LoadPosition(String name, @Nullable Side side) {
    public LoadPosition {
        Objects.requireNonNull(name, "Name cannot be null");
        if (name.isBlank()) {
            throw new DomainRuleViolationException("The name of a load position cannot be blank");
        }
    }
}
