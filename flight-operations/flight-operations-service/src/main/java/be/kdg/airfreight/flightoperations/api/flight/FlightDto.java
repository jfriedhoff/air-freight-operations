package be.kdg.airfreight.flightoperations.api.flight;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import be.kdg.airfreight.flightoperations.domain.flight.AirportCatalog;
import be.kdg.airfreight.flightoperations.domain.flight.DelayCodeCatalog;
import be.kdg.airfreight.flightoperations.domain.flight.EstimatedTimes;
import be.kdg.airfreight.flightoperations.domain.flight.Flight;
import be.kdg.airfreight.flightoperations.domain.flight.FlightStatus;
import org.jspecify.annotations.Nullable;

/** A flight as the details page needs it. All times are UTC; the page shows them in Brussels time. */
public record FlightDto(UUID id, String flightNumber, LocalDate scheduledDepartureDate,
                        AirportDto departureAirport, AirportDto arrivalAirport, String registration,
                        FlightStatus status, Instant std, Instant sta, Instant etd, Instant eta,
                        @Nullable Instant atd, @Nullable Instant ata, List<DelayDto> delays) {
    public static FlightDto from(Flight flight, AirportCatalog airports, DelayCodeCatalog delayCodes) {
        EstimatedTimes estimated = flight.getEstimated();
        return new FlightDto(flight.getId().id(), flight.getFlightNumber().value(),
            flight.getScheduled().departureDate(),
            AirportDto.from(flight.getDepartureAirport(), airports),
            AirportDto.from(flight.getArrivalAirport(), airports),
            flight.getRegistration().value(), flight.getStatus(),
            flight.getScheduled().departure(), flight.getScheduled().arrival(),
            estimated.departure(), estimated.arrival(),
            flight.getActualDeparture(), flight.getActualArrival(),
            flight.getDelays().stream().map(delay -> DelayDto.from(delay, delayCodes)).toList());
    }
}
