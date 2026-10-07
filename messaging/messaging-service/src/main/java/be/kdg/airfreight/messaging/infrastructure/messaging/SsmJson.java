package be.kdg.airfreight.messaging.infrastructure.messaging;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Contract network-planning.ssm, version 1. */
@JsonIgnoreProperties(ignoreUnknown = true)
record SsmJson(UUID messageId, String type, Integer version, Instant createdAt, LocalDate periodFrom,
               LocalDate periodTo, Set<DayOfWeek> weekdays, LocalTime departureTime, LocalTime arrivalTime,
               String flightNumber, String departureAirport, String arrivalAirport, String registration) {
}
