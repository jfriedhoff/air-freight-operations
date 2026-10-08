package be.kdg.airfreight.simulator.api;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import be.kdg.airfreight.simulator.application.ScheduleRecord;
import jakarta.validation.constraints.NotNull;

/**
 * One flight schedule record in our own JSON. Only "is it there" is checked: whether the values make
 * a valid SSM is for Messaging to judge, as with a message from the real system.
 */
public record ScheduleRecordDto(
    @NotNull(message = "periodFrom is required") LocalDate periodFrom,
    @NotNull(message = "periodTo is required") LocalDate periodTo,
    @NotNull(message = "weekdays is required") Set<DayOfWeek> weekdays,
    @NotNull(message = "departureTime is required") LocalTime departureTime,
    @NotNull(message = "arrivalTime is required") LocalTime arrivalTime,
    @NotNull(message = "flightNumber is required") String flightNumber,
    @NotNull(message = "departureAirport is required") String departureAirport,
    @NotNull(message = "arrivalAirport is required") String arrivalAirport,
    @NotNull(message = "registration is required") String registration) {

    ScheduleRecord toRecord() {
        return new ScheduleRecord(periodFrom, periodTo, weekdays, departureTime, arrivalTime, flightNumber,
            departureAirport, arrivalAirport, registration);
    }
}
