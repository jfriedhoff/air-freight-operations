package be.kdg.airfreight.loadplanning.domain.flightcargo;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FlightCargoRepository {
    Optional<FlightCargo> findById(FlightCargoId id);

    /** The cargo of a flight as the other contexts know it. */
    Optional<FlightCargo> findByFlightId(FlightId flightId);

    /** The cargos of the flights with this scheduled departure date, in order of STD and then flight number. */
    List<FlightCargo> findByScheduledDepartureDate(LocalDate scheduledDepartureDate);

    void save(FlightCargo flightCargo);

    void delete(FlightCargo flightCargo);
}
