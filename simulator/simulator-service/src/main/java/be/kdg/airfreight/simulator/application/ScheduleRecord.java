package be.kdg.airfreight.simulator.application;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import be.kdg.airfreight.simulator.infrastructure.messaging.SsmMessage;

/** One flight schedule record as the trainer supplies it. Times are UTC. */
public record ScheduleRecord(LocalDate periodFrom, LocalDate periodTo, Set<DayOfWeek> weekdays,
                             LocalTime departureTime, LocalTime arrivalTime, String flightNumber,
                             String departureAirport, String arrivalAirport, String registration) {
    public SsmMessage toSsm(UUID messageId, Instant createdAt) {
        return new SsmMessage(messageId, SsmMessage.TYPE, SsmMessage.VERSION, createdAt, periodFrom, periodTo,
            weekdays, departureTime, arrivalTime, flightNumber, departureAirport, arrivalAirport, registration);
    }
}
