package be.kdg.airfreight.flightoperations.acceptance.unit;

import be.kdg.airfreight.flightoperations.domain.DomainRuleViolationException;
import be.kdg.airfreight.flightoperations.domain.flight.ActualTimeTolerance;
import be.kdg.airfreight.flightoperations.domain.flight.AircraftRegistration;
import be.kdg.airfreight.flightoperations.domain.flight.AirportCode;
import be.kdg.airfreight.flightoperations.domain.flight.Flight;
import be.kdg.airfreight.flightoperations.domain.flight.FlightNumber;
import be.kdg.airfreight.flightoperations.domain.flight.ScheduledTimes;
import be.kdg.airfreight.flightoperations.domain.flightplanner.FlightPlannerId;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The state of one scenario, shared by all step classes. Picocontainer creates a new one for every scenario,
 * so nothing leaks from one scenario into the next. Never make these fields static.
 */
public class FlightWorld {
    static final String HJ221 = "HJ221";
    static final Instant NOW = Instant.parse("2026-01-09T05:00:00Z");
    static final LocalDate DEPARTURE_DATE = LocalDate.of(2026, Month.JANUARY, 9);
    static final ActualTimeTolerance TOLERANCE = new ActualTimeTolerance(Duration.ofMinutes(5));

    final Map<String, Flight> flights = new HashMap<>();
    final FlightPlannerId planner = FlightPlannerId.create();
    Instant now = NOW;
    Throwable error;
    String expectedRefusal;

    Flight flight(String flightNumber) {
        return flights.get(flightNumber);
    }

    void plan(String flightNumber, Instant std, Instant sta) {
        flights.put(flightNumber, Flight.plan(new FlightNumber(flightNumber), new AirportCode("BRU"),
            new AirportCode("ATH"), new AircraftRegistration("SXBAA"), new ScheduledTimes(std, sta)));
    }

    void assertRefusedForTheExpectedReason() {
        assertThat(error)
            .isInstanceOf(DomainRuleViolationException.class)
            .hasMessageContaining(expectedRefusal);
    }

    static Instant onDate(LocalDate date, String time) {
        return date.atTime(LocalTime.parse(time)).toInstant(ZoneOffset.UTC);
    }
}
