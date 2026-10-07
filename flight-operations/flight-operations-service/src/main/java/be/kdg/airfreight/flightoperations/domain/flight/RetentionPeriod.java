package be.kdg.airfreight.flightoperations.domain.flight;

import java.time.LocalDate;

import be.kdg.airfreight.flightoperations.domain.DomainRuleViolationException;
import org.jmolecules.ddd.annotation.ValueObject;

/** How many days after its scheduled departure date a flight is kept. */
@ValueObject
public record RetentionPeriod(int days) {
    public RetentionPeriod {
        if (days < 0) {
            throw new DomainRuleViolationException("The retention period cannot be negative");
        }
    }

    /** Flights scheduled on this date or before it have reached the end of their retention period. */
    public LocalDate lastExpiredDate(LocalDate today) {
        return today.minusDays(days);
    }
}
