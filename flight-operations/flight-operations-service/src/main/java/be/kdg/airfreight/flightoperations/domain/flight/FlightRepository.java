package be.kdg.airfreight.flightoperations.domain.flight;

import java.util.Optional;

public interface FlightRepository {
    Optional<Flight> findById(FlightId id);

    void save(Flight flight);
}
