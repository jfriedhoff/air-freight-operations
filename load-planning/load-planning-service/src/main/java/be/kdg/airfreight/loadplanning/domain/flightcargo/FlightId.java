package be.kdg.airfreight.loadplanning.domain.flightcargo;

import java.time.LocalDate;
import java.util.Objects;

import be.kdg.airfreight.loadplanning.domain.NotFoundException;
import org.jmolecules.ddd.annotation.ValueObject;

/** What identifies a flight in every context: flight number, scheduled departure date and departure airport. */
@ValueObject
public record FlightId(FlightNumber flightNumber, LocalDate scheduledDepartureDate, AirportCode departureAirport) {
    public FlightId {
        Objects.requireNonNull(flightNumber, "Flight number cannot be null");
        Objects.requireNonNull(scheduledDepartureDate, "Scheduled departure date cannot be null");
        Objects.requireNonNull(departureAirport, "Departure airport cannot be null");
    }

    /** Load Planning knows a flight by its cargo: a flight it was never told about has none. */
    public NotFoundException cargoNotFound() {
        return new NotFoundException("No cargo for flight " + flightNumber.value() + " of " + scheduledDepartureDate
            + " from " + departureAirport.value());
    }
}
