package be.kdg.airfreight.messaging.domain.booking;

import java.util.Arrays;
import java.util.Objects;

import be.kdg.airfreight.messaging.domain.DomainRuleViolationException;

/** The units a ULD message can give a weight in. */
public enum WeightUnit {
    KG, LB;

    public static WeightUnit of(String code) {
        Objects.requireNonNull(code, "Weight unit cannot be null");
        return Arrays.stream(values())
            .filter(unit -> unit.name().equals(code))
            .findFirst()
            .orElseThrow(() -> new DomainRuleViolationException("Weight unit '" + code
                + "' is invalid: expected KG or LB"));
    }
}
