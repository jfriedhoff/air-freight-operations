package be.kdg.airfreight.flightoperations.domain.flight;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FlightRepository {
    Optional<Flight> findById(FlightId id);

    /** A flight as the other contexts know it: by flight number, scheduled departure date and departure airport. */
    Optional<Flight> findByFlight(FlightNumber flightNumber, LocalDate scheduledDepartureDate,
                                  AirportCode departureAirport);

    List<Flight> findScheduledOnOrBefore(LocalDate scheduledDepartureDate);

    void save(Flight flight);

    void delete(Flight flight);
}
