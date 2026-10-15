package be.kdg.airfreight.flightoperations.domain.flight;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** A day is a UTC date: it starts at 00:00 UTC, also for the staff in Brussels (analysis, question 9). */
class ScheduledTimesTest {
    @Test
    void theLastSecondBeforeMidnightUtcStillBelongsToThatDay() {
        assertThat(ScheduledTimes.utcDateOf(Instant.parse("2026-01-09T23:59:59Z"))).isEqualTo(LocalDate.of(2026, 1, 9));
    }

    @Test
    void aNewDayStartsAtMidnightUtc() {
        assertThat(ScheduledTimes.utcDateOf(Instant.parse("2026-01-10T00:00:00Z"))).isEqualTo(LocalDate.of(2026, 1, 10));
    }

    @Test
    void aFlightAt2330UtcDepartsOnThatUtcDateAlthoughItIsTheNextDayInBrussels() {
        ScheduledTimes times = new ScheduledTimes(Instant.parse("2026-01-09T23:30:00Z"),
            Instant.parse("2026-01-10T02:45:00Z"));

        assertThat(times.departureDate()).isEqualTo(LocalDate.of(2026, 1, 9));
    }
}
