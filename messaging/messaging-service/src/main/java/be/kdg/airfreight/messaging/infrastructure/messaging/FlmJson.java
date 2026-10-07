package be.kdg.airfreight.messaging.infrastructure.messaging;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** The FLM for the partners, format version 1: one created flight. */
record FlmJson(UUID messageId, Instant createdAt, int formatVersion, String flightNumber,
               LocalDate scheduledDepartureDate, Instant std, Instant sta, String departureAirport,
               String arrivalAirport) {
}
