package be.kdg.airfreight.loadplanning.application.query;

import java.time.LocalDate;

import be.kdg.airfreight.loadplanning.domain.InvalidValueException;
import be.kdg.airfreight.loadplanning.domain.flightcargo.AirportCode;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightNumber;
import org.jspecify.annotations.Nullable;

/**
 * Which cargos the list of flights to load asks for: one UTC day (today when none is given), optionally the one
 * flight of that day with this flight number and departure airport, and whether departed flights are included.
 * The filters narrow the same list: Vluchtdetails finds the cargo of a departed flight by asking for departed ones.
 */
public record FlightsToLoadQuery(@Nullable LocalDate scheduledDepartureDate, @Nullable FlightNumber flightNumber,
                                 @Nullable AirportCode departureAirport, boolean includeDeparted) {
    public FlightsToLoadQuery {
        if ((flightNumber == null) != (departureAirport == null)) {
            throw new InvalidValueException(
                "A flight is found by its flight number and its departure airport together: give both or neither");
        }
    }

    public boolean byIdentity() {
        return flightNumber != null;
    }
}
