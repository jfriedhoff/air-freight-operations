package be.kdg.airfreight.flightoperations.infrastructure.messaging;

import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Contract messaging.cpm.created, version 1, as Flight Operations reads it. */
@JsonIgnoreProperties(ignoreUnknown = true)
record CpmCreatedJson(UUID messageId, String type, String flightNumber, LocalDate scheduledDepartureDate,
                      String departureAirport) {
}
