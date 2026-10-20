package be.kdg.airfreight.messaging.domain;

import java.util.Objects;
import java.util.regex.Pattern;

import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
public record FlightNumber(String value) {
    // An airline code of two characters with at least one letter (IATA) or of three letters (ICAO),
    // then one to four digits. Without the letter, 221 would read as airline 22 with flight 1.
    private static final Pattern FORMAT = Pattern.compile("(?:[A-Z][A-Z0-9]|[0-9][A-Z]|[A-Z]{3})\\d{1,4}");

    public FlightNumber {
        Objects.requireNonNull(value, "Flight number cannot be null");
        if (!FORMAT.matcher(value).matches()) {
            throw new DomainRuleViolationException("Flight number '" + value + "' is invalid: expected an airline "
                + "code of two characters with at least one letter, or of three letters, followed by one to four "
                + "digits, such as HJ221");
        }
    }
}
