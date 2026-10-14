package be.kdg.airfreight.flightoperations;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import be.kdg.airfreight.flightoperations.domain.flight.ActualTimeTolerance;
import be.kdg.airfreight.flightoperations.domain.flight.AircraftRegistration;
import be.kdg.airfreight.flightoperations.domain.flight.AirportCode;
import be.kdg.airfreight.flightoperations.domain.flight.DelayCode;
import be.kdg.airfreight.flightoperations.domain.flight.DelayCodeCatalog;
import be.kdg.airfreight.flightoperations.domain.flight.DelayDuration;
import be.kdg.airfreight.flightoperations.domain.flight.DelayKind;
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
import jakarta.persistence.EntityManager;
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
    @Autowired
    private DelayCodeCatalog delayCodes;
    @Autowired
    private EntityManager entityManager;

    /** HJ221 from BRU to ATH on 9 January 2026, STD 06:30 and STA 09:45 UTC. */
    public Flight savePlannedFlight() {
        Flight flight = Flight.plan(new FlightNumber("HJ221"), new AirportCode("BRU"), new AirportCode("ATH"),
            new AircraftRegistration("SXBAA"), new ScheduledTimes(STD, STA));
        flights.save(flight);
        return find(flight.getId());
    }

    /** As if the CPM of the flight was processed: from now on it may depart. */
    public void recordFinalizedLoad(FlightId id) {
        Flight flight = find(id);
        flight.recordFinalizedLoad(STD);
        flights.save(flight);
    }

    /** Finalizes the load of the flight and lets it depart at its STD. */
    public void depart(FlightId id) {
        Flight flight = find(id);
        flight.recordFinalizedLoad(STD);
        flight.registerDeparture(STD, saveFlightPlanner(), STD, new ActualTimeTolerance(Duration.ofMinutes(5)));
        flights.save(flight);
    }

    /** A flight between BRU and ATH, as in the stories. */
    public void saveFlight(String flightNumber, String departureAirport, LocalDate date, LocalTime std,
                           String registration) {
        flights.save(Flight.plan(new FlightNumber(flightNumber), new AirportCode(departureAirport),
            new AirportCode(otherAirport(departureAirport)), new AircraftRegistration(registration),
            schedule(date, std)));
    }

    /** The flights of the stories take 3 h 15 min, like STD 06:30 and STA 09:45. */
    public static ScheduledTimes schedule(LocalDate date, LocalTime std) {
        Instant departure = date.atTime(std).toInstant(ZoneOffset.UTC);
        return new ScheduledTimes(departure, departure.plus(Duration.between(STD, STA)));
    }

    /** The stories fly between BRU and ATH. */
    public static String otherAirport(String airport) {
        return airport.equals("ATH") ? "BRU" : "ATH";
    }

    public Optional<Flight> findFlight(String flightNumber, LocalDate date, String departureAirport) {
        return flights.findByFlight(new FlightNumber(flightNumber), date, new AirportCode(departureAirport));
    }

    public long flightCount() {
        return jpaFlights.count();
    }

    /** A departure delay of 25 minutes, reason 93 (late inbound aircraft). */
    public void addDepartureDelay(FlightId id) {
        Flight flight = find(id);
        flight.addDelay(DelayKind.DEPARTURE, new DelayDuration(25), new DelayCode("93"), "Late inbound aircraft",
            saveFlightPlanner(), STD, delayCodes);
        flights.save(flight);
    }

    /** Lands the departed flight at its STA. */
    public void land(FlightId id) {
        Flight flight = find(id);
        flight.registerArrival(STA, saveFlightPlanner(), STA, new ActualTimeTolerance(Duration.ofMinutes(5)));
        flights.save(flight);
    }

    public boolean exists(FlightId id) {
        return flights.findById(id).isPresent();
    }

    /** The delay rows of all flights: a delay can only be seen through its flight otherwise. */
    public long delayCount() {
        return entityManager.createQuery("select count(d) from JpaDelay d", Long.class).getSingleResult();
    }

    public Flight find(FlightId id) {
        return flights.findById(id).orElseThrow();
    }

    public FlightPlannerId saveFlightPlanner() {
        return saveFlightPlanner("Test flight planner");
    }

    public FlightPlannerId saveFlightPlanner(String name) {
        FlightPlannerId id = FlightPlannerId.create();
        flightPlanners.save(new FlightPlanner(id, name));
        return id;
    }

    public List<String> outboxTypes() {
        return outbox.findAll().stream().map(OutboxEvent::getType).toList();
    }

    public List<String> unsentOutboxTypes() {
        return outbox.findAll().stream().filter(event -> event.getSentAt() == null).map(OutboxEvent::getType).toList();
    }

    /** Planners stay: their ids are random, and the seeded one must survive. */
    public void cleanUp() {
        outbox.deleteAll();
        jpaFlights.deleteAll();
    }
}
