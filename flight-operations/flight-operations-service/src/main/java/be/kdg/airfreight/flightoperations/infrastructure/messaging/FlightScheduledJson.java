package be.kdg.airfreight.flightoperations.infrastructure.messaging;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Contract messaging.flight.scheduled, version 1, as Flight Operations reads it. */
@JsonIgnoreProperties(ignoreUnknown = true)
record FlightScheduledJson(UUID messageId, String type, String flightNumber, String departureAirport,
                           String arrivalAirport, String registration, Instant std, Instant sta) {
}
