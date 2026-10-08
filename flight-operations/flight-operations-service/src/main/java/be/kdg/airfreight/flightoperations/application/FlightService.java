package be.kdg.airfreight.flightoperations.application;

import java.time.Clock;
import java.util.List;

import be.kdg.airfreight.flightoperations.application.command.AddDelayCommand;
import be.kdg.airfreight.flightoperations.application.command.ChangeDelayCommand;
import be.kdg.airfreight.flightoperations.application.command.RegisterArrivalCommand;
import be.kdg.airfreight.flightoperations.application.command.RegisterDepartureCommand;
import be.kdg.airfreight.flightoperations.application.command.RemoveDelayCommand;
import be.kdg.airfreight.flightoperations.domain.flight.ActualTimeTolerance;
import be.kdg.airfreight.flightoperations.domain.flight.DelayCodeCatalog;
import be.kdg.airfreight.flightoperations.domain.flight.Flight;
import be.kdg.airfreight.flightoperations.domain.flight.FlightEvent;
import be.kdg.airfreight.flightoperations.domain.flight.FlightId;
import be.kdg.airfreight.flightoperations.domain.flight.FlightRepository;
import be.kdg.airfreight.flightoperations.domain.flightplanner.FlightPlannerId;
import be.kdg.airfreight.flightoperations.domain.flightplanner.FlightPlannerRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** What a flight planner does to a flight. Every rule is in Flight; this only frames the use case. */
@Service
@Transactional
public class FlightService {
    private final FlightRepository flights;
    private final FlightPlannerRepository flightPlanners;
    private final DelayCodeCatalog delayCodes;
    private final ApplicationEventPublisher events;
    private final ActualTimeTolerance tolerance;
    private final Clock clock;

    public FlightService(FlightRepository flights, FlightPlannerRepository flightPlanners,
                         DelayCodeCatalog delayCodes, ApplicationEventPublisher events,
                         ActualTimeTolerance tolerance, Clock clock) {
        this.flights = flights;
        this.flightPlanners = flightPlanners;
        this.delayCodes = delayCodes;
        this.events = events;
        this.tolerance = tolerance;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Flight get(FlightId id) {
        return find(id);
    }

    public Flight addDelay(AddDelayCommand command) {
        requireRegistered(command.by());
        Flight flight = find(command.flightId());
        return saved(flight, flight.addDelay(command.kind(), command.minutes(), command.reason(),
            command.description(), command.by(), clock.instant(), delayCodes));
    }

    public Flight changeDelay(ChangeDelayCommand command) {
        requireRegistered(command.by());
        Flight flight = find(command.flightId());
        return saved(flight, flight.changeDelay(command.delayId(), command.minutes(), command.description(),
            command.by(), clock.instant()));
    }

    public Flight removeDelay(RemoveDelayCommand command) {
        requireRegistered(command.by());
        Flight flight = find(command.flightId());
        return saved(flight, flight.removeDelay(command.delayId(), command.by(), clock.instant()));
    }

    public Flight registerDeparture(RegisterDepartureCommand command) {
        requireRegistered(command.by());
        Flight flight = find(command.flightId());
        return saved(flight, List.of(flight.registerDeparture(command.atd(), command.by(), clock.instant(),
            tolerance)));
    }

    public Flight registerArrival(RegisterArrivalCommand command) {
        requireRegistered(command.by());
        Flight flight = find(command.flightId());
        return saved(flight, List.of(flight.registerArrival(command.ata(), command.by(), clock.instant(),
            tolerance)));
    }

    /** A change is recorded with a planner that exists. Reading a second aggregate in the transaction is fine. */
    private void requireRegistered(FlightPlannerId planner) {
        if (flightPlanners.findById(planner).isEmpty()) {
            throw planner.notRegistered();
        }
    }

    private Flight find(FlightId id) {
        return flights.findById(id).orElseThrow(id::notFound);
    }

    /** The flight and what happened to it are saved together: the events are stored in this transaction. */
    private Flight saved(Flight flight, List<? extends FlightEvent> raised) {
        flights.save(flight);
        raised.forEach(events::publishEvent);
        return flight;
    }
}
