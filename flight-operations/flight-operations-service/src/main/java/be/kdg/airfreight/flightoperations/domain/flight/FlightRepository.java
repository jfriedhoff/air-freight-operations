package be.kdg.airfreight.flightoperations.domain.flight;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FlightRepository {
    Optional<Flight> findById(FlightId id);

    /** A flight as the other contexts know it: by flight number, scheduled departure date and departure airport. */
    Optional<Flight> findByFlight(FlightNumber flightNumber, LocalDate scheduledDepartureDate,
                                  AirportCode departureAirport);

    /** The flights with this scheduled departure date, in order of STD and then flight number. */
    List<Flight> findByScheduledDepartureDate(LocalDate scheduledDepartureDate);

    List<Flight> findScheduledOnOrBefore(LocalDate scheduledDepartureDate);

    void save(Flight flight);

    void delete(Flight flight);
}
