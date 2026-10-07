package be.kdg.airfreight.loadplanning.infrastructure.messaging;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Contract messaging.uld-booking.received, version 1, as Load Planning reads it. */
@JsonIgnoreProperties(ignoreUnknown = true)
record UldBookingReceivedJson(UUID messageId, String type, String flightNumber, LocalDate scheduledDepartureDate,
                              String departureAirport, String uldCode, BigDecimal weight, String weightUnit,
                              String destination) {
}
