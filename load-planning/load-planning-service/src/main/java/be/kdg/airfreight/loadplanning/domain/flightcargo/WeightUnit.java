package be.kdg.airfreight.loadplanning.domain.flightcargo;

import java.math.BigDecimal;

public enum WeightUnit {
    KG(BigDecimal.ONE),
    LB(new BigDecimal("0.45359237"));

    private final BigDecimal kilogramsPerUnit;

    WeightUnit(BigDecimal kilogramsPerUnit) {
        this.kilogramsPerUnit = kilogramsPerUnit;
    }

    BigDecimal toKilograms(BigDecimal value) {
        return value.multiply(kilogramsPerUnit);
    }
}
