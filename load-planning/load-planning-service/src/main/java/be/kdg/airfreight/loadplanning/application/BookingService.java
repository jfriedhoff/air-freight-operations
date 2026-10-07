package be.kdg.airfreight.loadplanning.application;

import java.time.Clock;

import be.kdg.airfreight.loadplanning.application.command.BookUldCommand;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargo;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class BookingService {
    private final FlightCargoRepository cargos;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public BookingService(FlightCargoRepository cargos, ApplicationEventPublisher events, Clock clock) {
        this.cargos = cargos;
        this.events = events;
        this.clock = clock;
    }

    /** A ULD is booked on a flight once. A second booking of the same code is ignored and logged. */
    @Transactional
    public void bookUld(BookUldCommand command) {
        FlightCargo cargo = cargos.findByFlightId(command.flightId())
            .orElseThrow(() -> FlightInfoService.notFound(command.flightId()));
        if (cargo.hasBooked(command.uldCode())) {
            log.warn("ULD {} is already booked on flight {} of {}: booking ignored", command.uldCode().value(),
                command.flightId().flightNumber().value(), command.flightId().scheduledDepartureDate());
            return;
        }
        events.publishEvent(cargo.book(command.uldCode(), command.weight(), command.destination(),
            clock.instant()));
        cargos.save(cargo);
        log.info("ULD {} booked on flight {} of {}", command.uldCode().value(),
            command.flightId().flightNumber().value(), command.flightId().scheduledDepartureDate());
    }
}
