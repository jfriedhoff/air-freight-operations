package be.kdg.airfreight.flightoperations;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import be.kdg.airfreight.flightoperations.domain.flight.ActualTimeTolerance;
import be.kdg.airfreight.flightoperations.domain.flight.AircraftRegistration;
import be.kdg.airfreight.flightoperations.domain.flight.AirportCode;
import be.kdg.airfreight.flightoperations.domain.flight.Flight;
import be.kdg.airfreight.flightoperations.domain.flight.FlightId;
import be.kdg.airfreight.flightoperations.domain.flight.FlightNumber;
import be.kdg.airfreight.flightoperations.domain.flight.FlightRepository;
import be.kdg.airfreight.flightoperations.domain.flight.ScheduledTimes;
import be.kdg.airfreight.flightoperations.domain.flightplanner.FlightPlanner;
import be.kdg.airfreight.flightoperations.domain.flightplanner.FlightPlannerId;
import be.kdg.airfreight.flightoperations.domain.flightplanner.FlightPlannerRepository;
import be.kdg.airfreight.flightoperations.infrastructure.flight.jpa.JpaFlightRepository;
import be.kdg.airfreight.flightoperations.infrastructure.messaging.OutboxEvent;
import be.kdg.airfreight.flightoperations.infrastructure.messaging.OutboxEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Puts flights and planners in the test database and empties it again. */
@Component
public class TestHelper {
    public static final Instant STD = Instant.parse("2026-01-09T06:30:00Z");
    public static final Instant STA = Instant.parse("2026-01-09T09:45:00Z");

    @Autowired
    private FlightRepository flights;
    @Autowired
    private FlightPlannerRepository flightPlanners;
    @Autowired
    private JpaFlightRepository jpaFlights;
    @Autowired
    private OutboxEventRepository outbox;

    /** HJ221 from BRU to ATH on 9 January 2026, STD 06:30 and STA 09:45 UTC. */
    public Flight savePlannedFlight() {
        Flight flight = Flight.plan(new FlightNumber("HJ221"), new AirportCode("BRU"), new AirportCode("ATH"),
            new AircraftRegistration("SXBAA"), new ScheduledTimes(STD, STA));
        flights.save(flight);
        return find(flight.getId());
    }

    /** Finalizes the load of the flight and lets it depart at its STD. */
    public void depart(FlightId id) {
        Flight flight = find(id);
        flight.recordFinalizedLoad(STD);
        flight.registerDeparture(STD, saveFlightPlanner(), STD, new ActualTimeTolerance(Duration.ofMinutes(5)));
        flights.save(flight);
    }

    public Flight find(FlightId id) {
        return flights.findById(id).orElseThrow();
    }

    public FlightPlannerId saveFlightPlanner() {
        FlightPlannerId id = FlightPlannerId.create();
        flightPlanners.save(new FlightPlanner(id, "Test flight planner"));
        return id;
    }

    public List<String> outboxTypes() {
        return outbox.findAll().stream().map(OutboxEvent::getType).toList();
    }

    /** Planners stay: their ids are random, and the seeded one must survive. */
    public void cleanUp() {
        outbox.deleteAll();
        jpaFlights.deleteAll();
    }
}
