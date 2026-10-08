package be.kdg.airfreight.simulator.infrastructure.messaging;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Contract network-planning.ssm, version 1: the schedule message as the network planning system sends
 * it. Messaging reads exactly this JSON, so a field only changes together with a new version.
 */
public record SsmMessage(UUID messageId, String type, int version, Instant createdAt, LocalDate periodFrom,
                         LocalDate periodTo, Set<DayOfWeek> weekdays,
                         @JsonFormat(pattern = "HH:mm") LocalTime departureTime,
                         @JsonFormat(pattern = "HH:mm") LocalTime arrivalTime, String flightNumber,
                         String departureAirport, String arrivalAirport, String registration) {
    /** Also the routing key the message travels with. */
    public static final String TYPE = "network-planning.ssm";
    public static final int VERSION = 1;
}
