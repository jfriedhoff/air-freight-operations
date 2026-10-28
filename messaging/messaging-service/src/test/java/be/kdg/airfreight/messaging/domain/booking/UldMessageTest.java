package be.kdg.airfreight.messaging.domain.booking;

import be.kdg.airfreight.messaging.domain.AirportCode;
import be.kdg.airfreight.messaging.domain.FlightId;
import be.kdg.airfreight.messaging.domain.FlightNumber;
import be.kdg.airfreight.messaging.domain.FormatVersion;
import be.kdg.airfreight.messaging.domain.MessageId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The event BookingService publishes for Load Planning: the ULD on the flight of the derived date. The rules of
 * a ULD message are checked by the @unit scenarios of story 29.
 */
class UldMessageTest {
    private static final AirportCode BRU = new AirportCode("BRU");
    private static final AirportCode ATH = new AirportCode("ATH");
    private static final Instant NOW = Instant.parse("2026-01-09T05:00:00Z");

    @Test
    void aUldMessageIsBookedOnTheFlightOfTheDerivedDate() {
        UldMessage message = new UldMessage(MessageId.create(), new FormatVersion(1), new FlightNumber("HJ221"), 9,
            BRU, new UldCode("AKE16689R7"), new BigDecimal("1250"), WeightUnit.KG, ATH);

        UldBookingReceived booking = message.bookingOn(LocalDate.of(2026, 1, 9), NOW);

        assertThat(booking.flightId())
            .isEqualTo(new FlightId(new FlightNumber("HJ221"), LocalDate.of(2026, 1, 9), BRU));
        assertThat(booking.uldCode()).isEqualTo("AKE16689R7");
        assertThat(booking.weight()).isEqualByComparingTo("1250");
        assertThat(booking.weightUnit()).isEqualTo("KG");
        assertThat(booking.destination()).isEqualTo(ATH);
        assertThat(booking.occurredOn()).isEqualTo(NOW);
    }
}
