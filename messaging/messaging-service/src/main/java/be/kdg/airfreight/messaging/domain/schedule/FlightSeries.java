package be.kdg.airfreight.messaging.domain.schedule;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import be.kdg.airfreight.messaging.domain.AircraftRegistration;
import be.kdg.airfreight.messaging.domain.AirportCode;
import be.kdg.airfreight.messaging.domain.DomainRuleViolationException;
import be.kdg.airfreight.messaging.domain.FlightNumber;
import org.jmolecules.ddd.annotation.ValueObject;

/** One route of an SSM: one flight number over a period, on certain weekdays, at fixed UTC times. */
@ValueObject
public record FlightSeries(SchedulePeriod period, Set<DayOfWeek> weekdays, LocalTime departureTime,
                           LocalTime arrivalTime, FlightNumber flightNumber, AirportCode departureAirport,
                           AirportCode arrivalAirport, AircraftRegistration registration) {
    public FlightSeries {
        Objects.requireNonNull(period, "Period cannot be null");
        Objects.requireNonNull(weekdays, "Weekdays cannot be null");
        Objects.requireNonNull(departureTime, "Departure time cannot be null");
        Objects.requireNonNull(arrivalTime, "Arrival time cannot be null");
        Objects.requireNonNull(flightNumber, "Flight number cannot be null");
        Objects.requireNonNull(departureAirport, "Departure airport cannot be null");
        Objects.requireNonNull(arrivalAirport, "Arrival airport cannot be null");
        Objects.requireNonNull(registration, "Registration cannot be null");
        if (weekdays.isEmpty()) {
            throw new DomainRuleViolationException("An SSM names at least one weekday");
        }
        if (departureAirport.equals(arrivalAirport)) {
            throw new DomainRuleViolationException("A flight cannot depart from and arrive at the same airport ("
                + departureAirport.value() + ")");
        }
        weekdays = Set.copyOf(weekdays);
    }

    /** One flight per day in the period whose weekday matches. */
    public List<FlightScheduled> flightsScheduled(Instant now) {
        return period.dates()
            .filter(date -> weekdays.contains(date.getDayOfWeek()))
            .map(date -> scheduledOn(date, now))
            .toList();
    }

    /** An arrival time at or before the departure time means the flight lands the next day. */
    private FlightScheduled scheduledOn(LocalDate date, Instant now) {
        LocalDate arrivalDate = arrivalTime.isAfter(departureTime) ? date : date.plusDays(1);
        Instant std = date.atTime(departureTime).toInstant(ZoneOffset.UTC);
        Instant sta = arrivalDate.atTime(arrivalTime).toInstant(ZoneOffset.UTC);
        return new FlightScheduled(flightNumber, departureAirport, arrivalAirport, registration, std, sta, now);
    }
}
