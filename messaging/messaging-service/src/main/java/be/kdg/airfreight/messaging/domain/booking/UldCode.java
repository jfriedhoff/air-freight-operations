package be.kdg.airfreight.messaging.domain.booking;

import java.util.Objects;
import java.util.regex.Pattern;

import be.kdg.airfreight.messaging.domain.DomainRuleViolationException;
import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
public record UldCode(String value) {
    private static final Pattern FORMAT = Pattern.compile("[A-Z]{3}\\d{4,5}[A-Z0-9]{2}");

    public UldCode {
        Objects.requireNonNull(value, "ULD code cannot be null");
        if (!FORMAT.matcher(value).matches()) {
            throw new DomainRuleViolationException("ULD code '" + value
                + "' is invalid: expected three letters, a serial number and an owner code, such as AKE16689R7");
        }
    }
}
