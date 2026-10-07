package be.kdg.airfreight.messaging.domain.telegram;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.stream.Stream;

import be.kdg.airfreight.messaging.domain.DomainRuleViolationException;
import org.jmolecules.ddd.annotation.Service;

/** Turns the day of the month and the time of day of a telegram into a full date and moment, in UTC. */
@Service
public class DateDerivation {

    /** The nearest date with that day of the month; at equal distance the future one. */
    public LocalDate deriveDay(int dayOfMonth, Instant readAt) {
        if (dayOfMonth < 1 || dayOfMonth > 31) {
            throw new DomainRuleViolationException("Day of month " + dayOfMonth + " is invalid");
        }
        LocalDate today = LocalDate.ofInstant(readAt, ZoneOffset.UTC);
        YearMonth thisMonth = YearMonth.from(today);
        YearMonth nearest = Stream.of(thisMonth.minusMonths(1), thisMonth, thisMonth.plusMonths(1))
            .min(Comparator.comparingLong((YearMonth month) -> daysBetween(today, month, dayOfMonth))
                .thenComparing(Comparator.<YearMonth>reverseOrder()))
            .orElseThrow();
        if (!nearest.isValidDay(dayOfMonth)) {
            throw new DomainRuleViolationException("Day " + dayOfMonth + " does not exist in " + nearest);
        }
        return nearest.atDay(dayOfMonth);
    }

    /** The nearest moment with that time of day, at most twelve hours away; at exactly twelve hours the later one. */
    public Instant deriveTime(LocalTime time, Instant readAt) {
        LocalDate today = LocalDate.ofInstant(readAt, ZoneOffset.UTC);
        return Stream.of(today.minusDays(1), today, today.plusDays(1))
            .map(date -> date.atTime(time).toInstant(ZoneOffset.UTC))
            .min(Comparator.comparing((Instant moment) -> Duration.between(readAt, moment).abs())
                .thenComparing(Comparator.<Instant>reverseOrder()))
            .orElseThrow();
    }

    /** Counted as if the day existed in that month: day 31 of April counts as the day after 30 April. */
    private static long daysBetween(LocalDate today, YearMonth month, int dayOfMonth) {
        return Math.abs(ChronoUnit.DAYS.between(today, month.atDay(1).plusDays(dayOfMonth - 1L)));
    }
}
