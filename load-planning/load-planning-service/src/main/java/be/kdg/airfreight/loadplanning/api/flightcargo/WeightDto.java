package be.kdg.airfreight.loadplanning.api.flightcargo;

import java.math.BigDecimal;

import be.kdg.airfreight.loadplanning.domain.flightcargo.Weight;
import be.kdg.airfreight.loadplanning.domain.flightcargo.WeightUnit;

/** A weight always travels with its unit. */
public record WeightDto(BigDecimal value, WeightUnit unit) {
    public static WeightDto from(Weight weight) {
        return new WeightDto(weight.value(), weight.unit());
    }
}
