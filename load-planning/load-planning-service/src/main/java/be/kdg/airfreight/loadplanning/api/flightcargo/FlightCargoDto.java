package be.kdg.airfreight.loadplanning.api.flightcargo;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import be.kdg.airfreight.loadplanning.application.FlightCargoSnapshot;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargo;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightInfo;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadPosition;

/**
 * The cargo of a flight as the cargo page needs it: the flight, every booked ULD in order of its code, and the
 * load positions of the floor plan in deck order for the selection list. All times are UTC; the page shows
 * them in Brussels time. loadCanChange says nothing about the role of the user.
 */
public record FlightCargoDto(UUID id, String flightNumber, LocalDate scheduledDepartureDate,
                             String departureAirport, String arrivalAirport, String aircraftType,
                             Instant std, Instant sta, Instant etd, Instant eta,
                             boolean departed, boolean finalized, LoadingPeriodDto loadingPeriod,
                             boolean loadCanChange, List<UldDto> ulds, List<String> loadPositions) {
    public static FlightCargoDto from(FlightCargoSnapshot snapshot) {
        FlightCargo cargo = snapshot.cargo();
        FlightInfo flight = cargo.getFlightInfo();
        return new FlightCargoDto(cargo.getId().id(), cargo.getFlightId().flightNumber().value(),
            cargo.getFlightId().scheduledDepartureDate(), cargo.getFlightId().departureAirport().value(),
            flight.arrivalAirport().value(), flight.aircraftType().code(),
            flight.std(), flight.sta(), flight.etd(), flight.eta(),
            flight.departed(), cargo.isFinalized(), LoadingPeriodDto.from(snapshot.loadingPeriod()),
            snapshot.loadCanChange(),
            cargo.getUlds().stream().map(UldDto::from).sorted(Comparator.comparing(UldDto::code)).toList(),
            snapshot.floorPlan().positions().stream().map(LoadPosition::name).toList());
    }
}
