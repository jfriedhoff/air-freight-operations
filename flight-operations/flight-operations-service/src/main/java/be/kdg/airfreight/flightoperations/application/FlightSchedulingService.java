package be.kdg.airfreight.flightoperations.application;

import java.time.Clock;
import java.time.LocalDate;

import be.kdg.airfreight.flightoperations.application.command.ScheduleFlightCommand;
import be.kdg.airfreight.flightoperations.domain.flight.Flight;
import be.kdg.airfreight.flightoperations.domain.flight.FlightRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class FlightSchedulingService {
    private final FlightRepository flights;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public FlightSchedulingService(FlightRepository flights, ApplicationEventPublisher events, Clock clock) {
        this.flights = flights;
        this.events = events;
        this.clock = clock;
    }

    /**
     * A flight from the schedule. One that already exists is not created again and not overwritten. That is the
     * normal answer to a repeated SSM, not a fault, so it is logged as info (story 39).
     */
    @Transactional
    public void scheduleFlight(ScheduleFlightCommand command) {
        LocalDate date = command.scheduled().departureDate();
        if (flights.findByFlight(command.flightNumber(), date, command.departureAirport()).isPresent()) {
            log.info("Flight {} of {} from {} already exists: not created again",
                command.flightNumber().value(), date, command.departureAirport().value());
            return;
        }
        Flight flight = Flight.plan(command.flightNumber(), command.departureAirport(), command.arrivalAirport(),
            command.registration(), command.scheduled());
        flights.save(flight);
        events.publishEvent(flight.announced(clock.instant()));
        log.info("Flight {} of {} from {} announced", command.flightNumber().value(), date,
            command.departureAirport().value());
    }
}
