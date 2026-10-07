package be.kdg.airfreight.messaging.infrastructure.messaging;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Contract messaging.cpm.created, version 1: the CPM of a flight is written, for Flight Operations. */
record CpmCreatedJson(UUID messageId, String type, int version, Instant occurredOn, String flightNumber,
                      LocalDate scheduledDepartureDate, String departureAirport) {
}
