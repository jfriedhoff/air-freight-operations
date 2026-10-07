package be.kdg.airfreight.messaging.infrastructure.messaging;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** The CPM for the partners, format version 1: the positioned ULDs of a finalized load. */
record CpmJson(UUID messageId, Instant createdAt, int formatVersion, String flightNumber,
               LocalDate scheduledDepartureDate, String departureAirport, List<UldLineJson> ulds) {
}
