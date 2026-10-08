package be.kdg.airfreight.flightoperations.domain.flight;

import java.util.Objects;
import java.util.regex.Pattern;

import be.kdg.airfreight.flightoperations.domain.InvalidValueException;
import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
public record FlightNumber(String value) {
    private static final Pattern FORMAT = Pattern.compile("[A-Z0-9]{2}\\d{1,4}");

    public FlightNumber {
        Objects.requireNonNull(value, "Flight number cannot be null");
        if (!FORMAT.matcher(value).matches()) {
            throw new InvalidValueException("Flight number '" + value
                + "' is invalid: expected two letters or digits followed by one to four digits, such as HJ221");
        }
    }
}
