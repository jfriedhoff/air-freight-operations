package be.kdg.airfreight.flightoperations.application;

import java.time.LocalDate;

import be.kdg.airfreight.flightoperations.domain.InvalidValueException;
import be.kdg.airfreight.flightoperations.domain.flight.AirportCode;
import be.kdg.airfreight.flightoperations.domain.flight.FlightNumber;
import org.jspecify.annotations.Nullable;

/**
 * Which flights the overview asks for: one UTC day (today when none is given) and, optionally, the one flight of
 * that day with this flight number and departure airport. That is how the other contexts know a flight.
 */
public record FlightsQuery(@Nullable LocalDate scheduledDepartureDate, @Nullable FlightNumber flightNumber,
                           @Nullable AirportCode departureAirport) {
    public FlightsQuery {
        if ((flightNumber == null) != (departureAirport == null)) {
            throw new InvalidValueException(
                "A flight is found by its flight number and its departure airport together: give both or neither");
        }
    }

    public boolean byIdentity() {
        return flightNumber != null;
    }
}
