package be.kdg.airfreight.flightoperations.acceptance.unit;

import be.kdg.airfreight.flightoperations.domain.DomainRuleViolationException;
import be.kdg.airfreight.flightoperations.domain.flight.ActualTimeTolerance;
import be.kdg.airfreight.flightoperations.domain.flight.AircraftRegistration;
import be.kdg.airfreight.flightoperations.domain.flight.AirportCode;
import be.kdg.airfreight.flightoperations.domain.flight.DelayCode;
import be.kdg.airfreight.flightoperations.domain.flight.DelayCodeCatalog;
import be.kdg.airfreight.flightoperations.domain.flight.DelayReason;
import be.kdg.airfreight.flightoperations.domain.flight.Flight;
import be.kdg.airfreight.flightoperations.domain.flight.FlightNumber;
import be.kdg.airfreight.flightoperations.domain.flight.ScheduledTimes;
import be.kdg.airfreight.flightoperations.domain.flightplanner.FlightPlannerId;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * The state of one scenario, shared by all step classes. Picocontainer creates a new one for every scenario,
 * so nothing leaks from one scenario into the next. Never make these fields static.
 */
public class FlightWorld {
    static final String HJ221 = "HJ221";
    static final Instant NOW = Instant.parse("2026-01-09T05:00:00Z");
    static final LocalDate DEPARTURE_DATE = LocalDate.of(2026, Month.JANUARY, 9);
    /** The schedule of a flight when the scenario does not name it. */
    static final LocalTime STD = LocalTime.of(6, 30);
    static final LocalTime STA = LocalTime.of(9, 45);
    static final ActualTimeTolerance TOLERANCE = new ActualTimeTolerance(Duration.ofMinutes(5));

    final Map<String, Flight> flights = new HashMap<>();
    final FlightPlannerId planner = FlightPlannerId.create();
    /** The IATA list of a scenario: only the delay reason the steps use. */
    final DelayCodeCatalog delayCodes = () -> List.of(new DelayReason(new DelayCode("93"), "Aircraft rotation"));
    Instant now = NOW;
    /** What the last When step was refused with, or null when it was accepted. */
    Throwable error;
    /** A fragment of the reason the scenario expects a refusal for, set by the step that causes it. */
    String expectedRefusal;

    Flight flight(String flightNumber) {
        return flights.get(flightNumber);
    }

    void plan(String flightNumber, Instant std, Instant sta) {
        flights.put(flightNumber, Flight.plan(new FlightNumber(flightNumber), new AirportCode("BRU"),
            new AirportCode("ATH"), new AircraftRegistration("SXBAA"), new ScheduledTimes(std, sta)));
    }

    /** Plans the flight on DEPARTURE_DATE with the default STD and STA. */
    void plan(String flightNumber) {
        plan(flightNumber, onDepartureDate(STD), onDepartureDate(STA));
    }

    /** Receives the CPM and registers the ATD, both at the ATD. */
    void depart(String flightNumber, Instant atd) {
        Flight flight = flight(flightNumber);
        flight.recordFinalizedLoad(atd);
        flight.registerDeparture(atd, planner, atd, TOLERANCE);
    }

    void land(String flightNumber, Instant ata) {
        flight(flightNumber).registerArrival(ata, planner, ata, TOLERANCE);
    }

    /** For the When steps: runs the action and keeps its refusal in error. */
    void attempt(ThrowingCallable action) {
        error = catchThrowable(action);
    }

    void assertAccepted() {
        assertThat(error).as("refusal").isNull();
    }

    void assertRefusedForTheExpectedReason() {
        assertThat(expectedRefusal).as("expected refusal, set by the step that causes it").isNotNull();
        assertThat(error)
            .isInstanceOf(DomainRuleViolationException.class)
            .hasMessageContaining(expectedRefusal);
    }

    static Instant onDate(LocalDate date, LocalTime time) {
        return date.atTime(time).toInstant(ZoneOffset.UTC);
    }

    static Instant onDepartureDate(LocalTime time) {
        return onDate(DEPARTURE_DATE, time);
    }
}
