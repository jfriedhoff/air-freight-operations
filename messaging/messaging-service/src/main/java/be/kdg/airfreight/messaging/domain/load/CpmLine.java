package be.kdg.airfreight.messaging.domain.load;

import java.math.BigDecimal;
import java.util.Objects;

import be.kdg.airfreight.messaging.domain.AirportCode;
import be.kdg.airfreight.messaging.domain.DomainRuleViolationException;
import org.jmolecules.ddd.annotation.ValueObject;

/** One line of a CPM: a ULD, where it stands, what it weighs and where it goes. */
@ValueObject
public record CpmLine(String uldCode, String position, BigDecimal weight, String weightUnit,
                      AirportCode destination) {
    public CpmLine {
        Objects.requireNonNull(uldCode, "ULD code cannot be null");
        Objects.requireNonNull(position, "Position cannot be null");
        Objects.requireNonNull(weight, "Weight cannot be null");
        Objects.requireNonNull(weightUnit, "Weight unit cannot be null");
        Objects.requireNonNull(destination, "Destination cannot be null");
        if (uldCode.isBlank() || position.isBlank() || weightUnit.isBlank()) {
            throw new DomainRuleViolationException("A CPM line needs a ULD code, a position and a weight unit");
        }
    }
}
