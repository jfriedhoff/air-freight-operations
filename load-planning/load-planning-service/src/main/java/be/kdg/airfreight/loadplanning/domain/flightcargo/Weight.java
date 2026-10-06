package be.kdg.airfreight.loadplanning.domain.flightcargo;

import java.math.BigDecimal;
import java.util.Objects;

import be.kdg.airfreight.loadplanning.domain.DomainRuleViolationException;
import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
public record Weight(BigDecimal value, WeightUnit unit) {
    public Weight {
        Objects.requireNonNull(value, "Weight value cannot be null");
        Objects.requireNonNull(unit, "Weight unit cannot be null");
        if (value.signum() <= 0) {
            throw new DomainRuleViolationException("A weight must be greater than 0");
        }
    }

    /** Exact conversion: nothing is rounded. */
    public BigDecimal toKilograms() {
        return unit.toKilograms(value);
    }
}
