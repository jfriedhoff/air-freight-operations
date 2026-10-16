package be.kdg.airfreight.loadplanning.api.flightcargo;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import be.kdg.airfreight.loadplanning.application.FlightCargoSnapshot;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargo;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightInfo;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadPosition;
import be.kdg.airfreight.loadplanning.domain.flightcargo.Uld;
import be.kdg.airfreight.loadplanning.domain.loadplanner.LoadPlannerId;
import org.jspecify.annotations.Nullable;

/**
 * The cargo of a flight as the cargo page needs it: the flight, every booked ULD in order of its code, and the
 * load positions of the floor plan in deck order for the selection list. All times are UTC; the page shows
 * them in Brussels time. loadCanChange says nothing about the role of the user. finalizedBy is the name of the
 * load planner who finalized the load.
 */
public record FlightCargoDto(UUID id, String flightNumber, LocalDate scheduledDepartureDate,
                             String departureAirport, String arrivalAirport, String aircraftType,
                             Instant std, Instant sta, Instant etd, Instant eta,
                             boolean departed, boolean finalized, @Nullable String finalizedBy,
                             LoadingPeriodDto loadingPeriod, boolean loadCanChange, List<UldDto> ulds,
                             List<String> loadPositions) {
    public static FlightCargoDto from(FlightCargoSnapshot snapshot, Map<LoadPlannerId, String> plannerNames) {
        FlightCargo cargo = snapshot.cargo();
        FlightInfo flight = cargo.getFlightInfo();
        LoadPlannerId finalizedBy = cargo.getFinalizedBy();
        return new FlightCargoDto(cargo.getId().id(), cargo.getFlightId().flightNumber().value(),
            cargo.getFlightId().scheduledDepartureDate(), cargo.getFlightId().departureAirport().value(),
            flight.arrivalAirport().value(), flight.aircraftType().code(),
            flight.std(), flight.sta(), flight.etd(), flight.eta(),
            flight.departed(), cargo.isFinalized(), finalizedBy == null ? null : plannerNames.get(finalizedBy),
            LoadingPeriodDto.from(snapshot.loadingPeriod()), snapshot.loadCanChange(),
            cargo.getUlds().stream().map(uld -> UldDto.from(uld, plannerNames))
                .sorted(Comparator.comparing(UldDto::code)).toList(),
            snapshot.floorPlan().positions().stream().map(LoadPosition::name).toList());
    }

    /** Every load planner whose name the page shows: who placed each ULD and who finalized the load. */
    public static Set<LoadPlannerId> plannersOf(FlightCargo cargo) {
        Set<LoadPlannerId> planners = new HashSet<>();
        cargo.getUlds().stream().map(Uld::getPositionChangedBy).filter(Objects::nonNull).forEach(planners::add);
        LoadPlannerId finalizedBy = cargo.getFinalizedBy();
        if (finalizedBy != null) {
            planners.add(finalizedBy);
        }
        return planners;
    }
}
