package be.kdg.airfreight.loadplanning.acceptance.common;

import be.kdg.airfreight.loadplanning.domain.flightcargo.Weight;
import be.kdg.airfreight.loadplanning.domain.flightcargo.WeightUnit;

import java.math.BigDecimal;
import java.util.Locale;

/** Weights as the stories write them ({weight}), for the unit and the integration glue. */
public final class Weights {
    private Weights() {
    }

    /** "2646 lb" becomes a Weight of 2646 LB. */
    public static Weight of(String weight) {
        String[] parts = weight.split(" ");
        return new Weight(new BigDecimal(parts[0]), WeightUnit.valueOf(parts[1].toUpperCase(Locale.ROOT)));
    }
}
