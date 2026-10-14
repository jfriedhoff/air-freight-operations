package be.kdg.airfreight.loadplanning.api.flightcargo;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargo;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightInfo;

/** One row of the list of flights to load: the flight without its ULDs. The id opens the cargo page. */
public record FlightToLoadDto(UUID id, String flightNumber, LocalDate scheduledDepartureDate,
                              String departureAirport, String arrivalAirport,
                              Instant std, Instant sta, Instant etd, Instant eta,
                              boolean departed, boolean finalized) {
    public static FlightToLoadDto from(FlightCargo cargo) {
        FlightInfo flight = cargo.getFlightInfo();
        return new FlightToLoadDto(cargo.getId().id(), cargo.getFlightId().flightNumber().value(),
            cargo.getFlightId().scheduledDepartureDate(), cargo.getFlightId().departureAirport().value(),
            flight.arrivalAirport().value(), flight.std(), flight.sta(), flight.etd(), flight.eta(),
            flight.departed(), cargo.isFinalized());
    }
}
