package be.kdg.airfreight.messaging.infrastructure.messaging;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Contract cargo-booking.uld, version 1. The flight is named by the day of the month only. */
@JsonIgnoreProperties(ignoreUnknown = true)
record UldMessageJson(UUID messageId, String type, Integer version, Instant createdAt, String flightNumber,
                      Integer dayOfMonth, String departureAirport, String uldCode, BigDecimal weight,
                      String weightUnit, String destination) {
}
