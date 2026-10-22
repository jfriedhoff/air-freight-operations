package be.kdg.airfreight.loadplanning.domain.flightcargo;

import java.math.BigDecimal;
import java.util.Objects;

import org.jmolecules.ddd.annotation.ValueObject;
import org.jspecify.annotations.Nullable;

/**
 * The weight on each side and the most the heaviest side may carry, all in kg and not rounded, as the lateral
 * balance check computed them. The heavier side is the one that weighs more; with equal sides there is none.
 */
@ValueObject
public record LateralBalanceDetails(BigDecimal leftKilograms, BigDecimal rightKilograms,
                                    BigDecimal maximumOnTheHeaviestSide, @Nullable Side heavierSide)
    implements LoadCheckDetails {
    public LateralBalanceDetails {
        Objects.requireNonNull(leftKilograms, "Left weight cannot be null");
        Objects.requireNonNull(rightKilograms, "Right weight cannot be null");
        Objects.requireNonNull(maximumOnTheHeaviestSide, "Maximum cannot be null");
    }
}
