package be.kdg.airfreight.flightoperations.application;

import java.time.Clock;
import java.util.List;

import be.kdg.airfreight.flightoperations.application.command.AddDelayCommand;
import be.kdg.airfreight.flightoperations.application.command.ChangeDelayCommand;
import be.kdg.airfreight.flightoperations.application.command.RegisterArrivalCommand;
import be.kdg.airfreight.flightoperations.application.command.RegisterDepartureCommand;
import be.kdg.airfreight.flightoperations.application.command.RemoveDelayCommand;
import be.kdg.airfreight.flightoperations.domain.flight.ActualTimeTolerance;
import be.kdg.airfreight.flightoperations.domain.flight.Flight;
import be.kdg.airfreight.flightoperations.domain.flight.FlightEvent;
import be.kdg.airfreight.flightoperations.domain.flight.FlightId;
import be.kdg.airfreight.flightoperations.domain.flight.FlightRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** What a flight planner does to a flight. Every rule is in Flight; this only frames the use case. */
@Service
@Transactional
public class FlightService {
    private final FlightRepository flights;
    private final ApplicationEventPublisher events;
    private final ActualTimeTolerance tolerance;
    private final Clock clock;

    public FlightService(FlightRepository flights, ApplicationEventPublisher events,
                         ActualTimeTolerance tolerance, Clock clock) {
        this.flights = flights;
        this.events = events;
        this.tolerance = tolerance;
        this.clock = clock;
    }

    public Flight addDelay(AddDelayCommand command) {
        Flight flight = find(command.flightId());
        return saved(flight, flight.addDelay(command.kind(), command.minutes(), command.reason(),
            command.description(), command.by(), clock.instant()));
    }

    public Flight changeDelay(ChangeDelayCommand command) {
        Flight flight = find(command.flightId());
        return saved(flight, flight.changeDelay(command.delayId(), command.minutes(), command.description(),
            command.by(), clock.instant()));
    }

    public Flight removeDelay(RemoveDelayCommand command) {
        Flight flight = find(command.flightId());
        return saved(flight, flight.removeDelay(command.delayId(), command.by(), clock.instant()));
    }

    public Flight registerDeparture(RegisterDepartureCommand command) {
        Flight flight = find(command.flightId());
        return saved(flight, List.of(flight.registerDeparture(command.atd(), command.by(), clock.instant(),
            tolerance)));
    }

    public Flight registerArrival(RegisterArrivalCommand command) {
        Flight flight = find(command.flightId());
        return saved(flight, List.of(flight.registerArrival(command.ata(), command.by(), clock.instant(),
            tolerance)));
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
