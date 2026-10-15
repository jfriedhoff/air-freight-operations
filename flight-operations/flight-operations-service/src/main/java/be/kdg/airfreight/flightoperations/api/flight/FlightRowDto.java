package be.kdg.airfreight.flightoperations.api.flight;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import be.kdg.airfreight.flightoperations.domain.flight.Flight;

/**
 * One row of the overview: the identity of a flight and its scheduled times. The id opens Vluchtdetails. Times are
 * UTC; the page shows them in Brussels time. The scheduled departure date is a UTC date.
 */
public record FlightRowDto(UUID id, String flightNumber, LocalDate scheduledDepartureDate,
                           String departureAirport, String arrivalAirport, Instant std, Instant sta) {
    public static FlightRowDto from(Flight flight) {
        return new FlightRowDto(flight.getId().id(), flight.getFlightNumber().value(),
            flight.getScheduled().departureDate(), flight.getDepartureAirport().value(),
            flight.getArrivalAirport().value(), flight.getScheduled().departure(), flight.getScheduled().arrival());
    }
}
