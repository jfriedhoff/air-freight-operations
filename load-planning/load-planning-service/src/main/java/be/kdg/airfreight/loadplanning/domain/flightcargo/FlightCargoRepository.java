package be.kdg.airfreight.loadplanning.domain.flightcargo;

import java.util.Optional;

public interface FlightCargoRepository {
    Optional<FlightCargo> findById(FlightCargoId id);

    void save(FlightCargo flightCargo);
}
