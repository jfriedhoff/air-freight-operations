package be.kdg.airfreight.simulator.infrastructure.messaging;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import be.kdg.airfreight.simulator.application.BookingRecord;

/**
 * Contract cargo-booking.uld, version 1: one booked ULD as the booking system sends it. The flight is
 * named by the day of the month only, as in a telegram.
 */
public record UldMessage(UUID messageId, String type, int version, Instant createdAt, String flightNumber,
                         int dayOfMonth, String departureAirport, String uldCode, BigDecimal weight,
                         String weightUnit, String destination) {
    /** Also the routing key the message travels with. */
    public static final String TYPE = "cargo-booking.uld";
    public static final int VERSION = 1;

    /** The real booking system only sends the day of the month; Messaging derives the date again. */
    public static UldMessage of(BookingRecord booking, UUID messageId, Instant createdAt) {
        return new UldMessage(messageId, TYPE, VERSION, createdAt, booking.flightNumber(),
            booking.scheduledDepartureDate().getDayOfMonth(), booking.departureAirport(), booking.uldCode(),
            booking.weight(), booking.weightUnit(), booking.destination());
    }
}
