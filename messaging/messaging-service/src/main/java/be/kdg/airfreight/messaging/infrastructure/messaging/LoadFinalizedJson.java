package be.kdg.airfreight.messaging.infrastructure.messaging;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Contract load-planning.flight-cargo.load-finalized, version 1. */
@JsonIgnoreProperties(ignoreUnknown = true)
record LoadFinalizedJson(UUID messageId, String type, Integer version, Instant occurredOn, String flightNumber,
                         LocalDate scheduledDepartureDate, String departureAirport, List<UldLineJson> load) {
}
