package be.kdg.airfreight.messaging.infrastructure.messaging;

import java.time.Instant;
import java.util.UUID;

/** Contract messaging.flight.scheduled, version 1: one flight from an SSM, for Flight Operations. */
record FlightScheduledJson(UUID messageId, String type, int version, Instant occurredOn, String flightNumber,
                           String departureAirport, String arrivalAirport, String registration, Instant std,
                           Instant sta) {
}
