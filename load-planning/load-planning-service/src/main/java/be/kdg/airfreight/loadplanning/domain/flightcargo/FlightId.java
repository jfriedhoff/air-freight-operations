package be.kdg.airfreight.loadplanning.domain.flightcargo;

import java.time.LocalDate;
import java.util.Objects;

import org.jmolecules.ddd.annotation.ValueObject;

/** What identifies a flight in every context: flight number, scheduled departure date and departure airport. */
@ValueObject
public record FlightId(FlightNumber flightNumber, LocalDate scheduledDepartureDate, AirportCode departureAirport) {
    public FlightId {
        Objects.requireNonNull(flightNumber, "Flight number cannot be null");
        Objects.requireNonNull(scheduledDepartureDate, "Scheduled departure date cannot be null");
        Objects.requireNonNull(departureAirport, "Departure airport cannot be null");
    }
}
