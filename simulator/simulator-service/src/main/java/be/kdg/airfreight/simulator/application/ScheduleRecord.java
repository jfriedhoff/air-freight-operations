package be.kdg.airfreight.simulator.application;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

/** One flight schedule record as the trainer supplies it. Times are UTC. */
public record ScheduleRecord(LocalDate periodFrom, LocalDate periodTo, Set<DayOfWeek> weekdays,
                             LocalTime departureTime, LocalTime arrivalTime, String flightNumber,
                             String departureAirport, String arrivalAirport, String registration) {
}
