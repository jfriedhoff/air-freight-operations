package be.kdg.airfreight.loadplanning.domain.flightcargo;

import java.util.Optional;

public interface FlightCargoRepository {
    Optional<FlightCargo> findById(FlightCargoId id);

    /** The cargo of a flight as the other contexts know it. */
    Optional<FlightCargo> findByFlightId(FlightId flightId);

    void save(FlightCargo flightCargo);

    void delete(FlightCargo flightCargo);
}
