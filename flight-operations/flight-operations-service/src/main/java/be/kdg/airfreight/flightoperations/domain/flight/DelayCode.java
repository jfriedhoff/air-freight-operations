package be.kdg.airfreight.flightoperations.domain.flight;

import java.util.Objects;
import java.util.regex.Pattern;

import be.kdg.airfreight.flightoperations.domain.InvalidValueException;
import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
public record DelayCode(String value) {
    private static final Pattern FORMAT = Pattern.compile("\\d{2}");

    public DelayCode {
        Objects.requireNonNull(value, "Delay code cannot be null");
        if (!FORMAT.matcher(value).matches()) {
            throw new InvalidValueException("Delay reason '" + value
                + "' is invalid: expected a two-digit IATA code, such as 93");
        }
    }
}
