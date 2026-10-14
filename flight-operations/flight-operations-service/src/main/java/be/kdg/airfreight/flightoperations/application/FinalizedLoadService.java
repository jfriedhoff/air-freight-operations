package be.kdg.airfreight.flightoperations.application;

import java.time.Clock;

import be.kdg.airfreight.flightoperations.application.command.RecordFinalizedLoadCommand;
import be.kdg.airfreight.flightoperations.domain.NotFoundException;
import be.kdg.airfreight.flightoperations.domain.flight.FinalizedLoadReceived;
import be.kdg.airfreight.flightoperations.domain.flight.Flight;
import be.kdg.airfreight.flightoperations.domain.flight.FlightRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class FinalizedLoadService {
    private final FlightRepository flights;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public FinalizedLoadService(FlightRepository flights, ApplicationEventPublisher events, Clock clock) {
        this.flights = flights;
        this.events = events;
        this.clock = clock;
    }

    /** The CPM of a flight exists: from now on its departure can be registered. Hearing it twice changes nothing. */
    @Transactional
    public void recordFinalizedLoad(RecordFinalizedLoadCommand command) {
        Flight flight = flights.findByFlight(command.flightNumber(), command.scheduledDepartureDate(),
                command.departureAirport())
            .orElseThrow(() -> new NotFoundException("Flight " + command.flightNumber().value() + " of "
                + command.scheduledDepartureDate() + " from " + command.departureAirport().value() + " not found"));
        if (flight.isFinalizedLoadReceived()) {
            log.info("Finalized load of flight {} of {} was already recorded: nothing changed",
                command.flightNumber().value(), command.scheduledDepartureDate());
            return;
        }
        FinalizedLoadReceived recorded = flight.recordFinalizedLoad(clock.instant());
        flights.save(flight);
        events.publishEvent(recorded);
        log.info("Finalized load recorded for flight {} of {}", command.flightNumber().value(),
            command.scheduledDepartureDate());
    }
}
