package be.kdg.airfreight.flightoperations.api.flight;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import be.kdg.airfreight.flightoperations.domain.flight.AirportCatalog;
import be.kdg.airfreight.flightoperations.domain.flight.DelayCodeCatalog;
import be.kdg.airfreight.flightoperations.domain.flight.EstimatedTimes;
import be.kdg.airfreight.flightoperations.domain.flight.Flight;
import be.kdg.airfreight.flightoperations.domain.flight.FlightStatus;
import be.kdg.airfreight.flightoperations.domain.flightplanner.FlightPlannerId;
import org.jspecify.annotations.Nullable;

/**
 * A flight as the details page needs it. All times are UTC; the page shows them in Brussels time.
 * atdRegisteredBy and ataRegisteredBy are the names of the flight planners who registered them.
 */
public record FlightDto(UUID id, String flightNumber, LocalDate scheduledDepartureDate,
                        AirportDto departureAirport, AirportDto arrivalAirport, String registration,
                        FlightStatus status, Instant std, Instant sta, Instant etd, Instant eta,
                        @Nullable Instant atd, @Nullable String atdRegisteredBy,
                        @Nullable Instant ata, @Nullable String ataRegisteredBy, List<DelayDto> delays) {
    public static FlightDto from(Flight flight, AirportCatalog airports, DelayCodeCatalog delayCodes,
                                 Map<FlightPlannerId, String> plannerNames) {
        EstimatedTimes estimated = flight.getEstimated();
        return new FlightDto(flight.getId().id(), flight.getFlightNumber().value(),
            flight.getScheduled().departureDate(),
            AirportDto.from(flight.getDepartureAirport(), airports),
            AirportDto.from(flight.getArrivalAirport(), airports),
            flight.getRegistration().value(), flight.getStatus(),
            flight.getScheduled().departure(), flight.getScheduled().arrival(),
            estimated.departure(), estimated.arrival(),
            flight.getActualDeparture(), nameOf(flight.getDepartureRegisteredBy(), plannerNames),
            flight.getActualArrival(), nameOf(flight.getArrivalRegisteredBy(), plannerNames),
            flight.getDelays().stream().map(delay -> DelayDto.from(delay, delayCodes, plannerNames)).toList());
    }

    /** Every flight planner whose name the page shows: who changed a delay and who registered ATD or ATA. */
    public static Set<FlightPlannerId> plannersOf(Flight flight) {
        Set<FlightPlannerId> planners = new HashSet<>();
        flight.getDelays().forEach(delay -> planners.add(delay.getLastChangedBy()));
        Stream.of(flight.getDepartureRegisteredBy(), flight.getArrivalRegisteredBy())
            .filter(Objects::nonNull).forEach(planners::add);
        return planners;
    }

    private static @Nullable String nameOf(@Nullable FlightPlannerId planner, Map<FlightPlannerId, String> names) {
        return planner == null ? null : names.get(planner);
    }
}
