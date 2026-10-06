package be.kdg.airfreight.messaging.infrastructure.messaging;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Contract messaging.uld-booking.received, version 1: one booked ULD, for Load Planning. */
record UldBookingReceivedJson(UUID messageId, String type, int version, Instant occurredOn, String flightNumber,
                              LocalDate scheduledDepartureDate, String departureAirport, String uldCode,
                              BigDecimal weight, String weightUnit, String destination) {
}
