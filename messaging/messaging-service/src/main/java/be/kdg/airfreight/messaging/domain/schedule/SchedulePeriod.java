package be.kdg.airfreight.messaging.domain.schedule;

import java.time.LocalDate;
import java.util.Objects;
import java.util.stream.Stream;

import be.kdg.airfreight.messaging.domain.DomainRuleViolationException;
import org.jmolecules.ddd.annotation.ValueObject;

/** The period of an SSM: the first and the last day are both part of it. */
@ValueObject
public record SchedulePeriod(LocalDate from, LocalDate to) {
    public SchedulePeriod {
        Objects.requireNonNull(from, "Start of the period cannot be null");
        Objects.requireNonNull(to, "End of the period cannot be null");
        if (from.isAfter(to)) {
            throw new DomainRuleViolationException("The period of an SSM cannot end before it starts");
        }
    }

    public Stream<LocalDate> dates() {
        return from.datesUntil(to.plusDays(1));
    }
}
