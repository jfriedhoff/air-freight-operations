package be.kdg.airfreight.messaging.infrastructure.messaging;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** The FLM for the partners, format version 1: one created flight. Field names follow the partner examples. */
record FlmJson(UUID messageId, Instant producedAt, int version, String type, String flightNumber,
               LocalDate scheduledDepartureDate, Instant scheduledTimeOfDeparture,
               Instant scheduledTimeOfArrival, String departureAirport, String arrivalAirport) {
}
