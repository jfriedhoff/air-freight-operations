package be.kdg.airfreight.loadplanning.infrastructure.messaging;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.jspecify.annotations.Nullable;

/**
 * Contracts messaging.flight.*, version 1, as Load Planning reads them: which flight, and of the other
 * fields only those this context uses. A field that an event does not have is null. occurredOn is when the
 * change happened in Flight Operations; Messaging passes it on unchanged.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record FlightEventJson(UUID messageId, String type, @Nullable Instant occurredOn, String flightNumber,
                       LocalDate scheduledDepartureDate, String departureAirport, @Nullable String arrivalAirport,
                       @Nullable Instant std, @Nullable Instant sta, @Nullable Instant etd, @Nullable Instant eta) {
}
