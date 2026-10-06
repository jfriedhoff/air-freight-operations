package be.kdg.airfreight.messaging.domain;

import java.util.Objects;
import java.util.regex.Pattern;

import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
public record AirportCode(String value) {
    private static final Pattern FORMAT = Pattern.compile("[A-Z]{3}");

    public AirportCode {
        Objects.requireNonNull(value, "Airport code cannot be null");
        if (!FORMAT.matcher(value).matches()) {
            throw new DomainRuleViolationException("Airport code '" + value
                + "' is invalid: expected three capital letters, such as BRU");
        }
    }
}
