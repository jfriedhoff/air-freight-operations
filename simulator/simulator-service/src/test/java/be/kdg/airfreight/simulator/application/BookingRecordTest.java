package be.kdg.airfreight.simulator.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import be.kdg.airfreight.simulator.infrastructure.messaging.UldMessage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BookingRecordTest {
    private static final UUID MESSAGE_ID = UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final Instant NOW = Instant.parse("2026-01-02T08:00:00Z");

    @Test
    void theScheduledDepartureDateBecomesTheDayOfTheMonth() {
        UldMessage message = bookingOn(LocalDate.of(2026, 1, 9)).toUldMessage(MESSAGE_ID, NOW);

        assertThat(message.dayOfMonth()).isEqualTo(9);
    }

    @Test
    void theLastDayOfALongMonthStaysThe31st() {
        UldMessage message = bookingOn(LocalDate.of(2026, 1, 31)).toUldMessage(MESSAGE_ID, NOW);

        assertThat(message.dayOfMonth()).isEqualTo(31);
    }

    @Test
    void aBookingRecordBecomesAUldMessageWithTheSameUld() {
        UldMessage message = bookingOn(LocalDate.of(2026, 1, 9)).toUldMessage(MESSAGE_ID, NOW);

        assertThat(message).isEqualTo(new UldMessage(MESSAGE_ID, "cargo-booking.uld", 1, NOW, "HJ221", 9, "BRU",
            "AKE16689R7", new BigDecimal("1250.5"), "KG", "ATH"));
    }

    private static BookingRecord bookingOn(LocalDate scheduledDepartureDate) {
        return new BookingRecord("HJ221", scheduledDepartureDate, "BRU", "AKE16689R7", new BigDecimal("1250.5"),
            "KG", "ATH");
    }
}
