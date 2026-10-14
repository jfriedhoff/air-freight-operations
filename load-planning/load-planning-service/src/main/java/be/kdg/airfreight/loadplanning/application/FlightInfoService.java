package be.kdg.airfreight.loadplanning.application;

import java.time.Clock;
import java.time.Instant;
import java.util.function.UnaryOperator;

import be.kdg.airfreight.loadplanning.application.command.AnnounceFlightCommand;
import be.kdg.airfreight.loadplanning.domain.flightcargo.AircraftType;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargo;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargoRepository;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightId;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightInfo;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadingPolicy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps this context's copy of each flight up to date from the flight events. The events of a flight
 * arrive in the order they happened, so each one is simply applied to the copy we hold.
 */
@Slf4j
@Service
@Transactional
public class FlightInfoService {
    private final FlightCargoRepository cargos;
    private final AircraftType aircraftType;
    private final LoadingPolicy policy;
    private final Clock clock;

    public FlightInfoService(FlightCargoRepository cargos, AircraftType aircraftType, LoadingPolicy policy,
                             Clock clock) {
        this.cargos = cargos;
        this.aircraftType = aircraftType;
        this.policy = policy;
        this.clock = clock;
    }

    /** A new flight gets an empty cargo. One that already has a cargo keeps it. */
    public void flightAnnounced(AnnounceFlightCommand command) {
        FlightId flightId = command.flightId();
        if (cargos.findByFlightId(flightId).isPresent()) {
            log.warn("Flight {} of {} already has a cargo: not created again", flightId.flightNumber().value(),
                flightId.scheduledDepartureDate());
            return;
        }
        cargos.save(FlightCargo.create(flightId, FlightInfo.scheduled(command.arrivalAirport(), aircraftType,
            command.std(), command.sta()), policy));
        log.info("Cargo created for flight {} of {}", flightId.flightNumber().value(),
            flightId.scheduledDepartureDate());
    }

    /** A new ETD moves the end of the loading period, as long as it has not ended. */
    public void departureEstimateChanged(FlightId flightId, Instant etd) {
        FlightCargo cargo = cargos.findByFlightId(flightId).orElseThrow(flightId::cargoNotFound);
        cargo.changeEstimatedDeparture(etd, clock.instant(), policy);
        cargos.save(cargo);
    }

    public void arrivalEstimateChanged(FlightId flightId, Instant eta) {
        update(flightId, info -> info.withEstimatedArrival(eta));
    }

    /** From now on the load of this flight is fixed and nothing can be booked on it. */
    public void flightDeparted(FlightId flightId) {
        update(flightId, FlightInfo::asDeparted);
    }

    /** The flight is gone after its retention period: its cargo goes with it. */
    public void flightRemoved(FlightId flightId) {
        cargos.findByFlightId(flightId).ifPresentOrElse(
            cargo -> {
                cargos.delete(cargo);
                log.info("Cargo of flight {} of {} removed", flightId.flightNumber().value(),
                    flightId.scheduledDepartureDate());
            },
            () -> log.info("No cargo to remove for flight {} of {}", flightId.flightNumber().value(),
                flightId.scheduledDepartureDate()));
    }

    private void update(FlightId flightId, UnaryOperator<FlightInfo> change) {
        FlightCargo cargo = cargos.findByFlightId(flightId).orElseThrow(flightId::cargoNotFound);
        cargo.updateFlightInfo(change.apply(cargo.getFlightInfo()));
        cargos.save(cargo);
    }
}
