package be.kdg.airfreight.loadplanning.domain.flightcargo;

import java.math.BigDecimal;
import java.util.Objects;

import be.kdg.airfreight.loadplanning.domain.InvalidValueException;
import org.jmolecules.ddd.annotation.ValueObject;

/** How much heavier the heaviest side may be than the lightest, as a fraction: 0.10 for 10%. */
@ValueObject
public record LateralBalanceLimit(BigDecimal fraction) {
    public LateralBalanceLimit {
        Objects.requireNonNull(fraction, "Fraction cannot be null");
        if (fraction.signum() < 0) {
            throw new InvalidValueException("The lateral balance limit cannot be negative");
        }
    }
}
