package be.kdg.airfreight.loadplanning.domain.flightcargo.check;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import be.kdg.airfreight.loadplanning.domain.flightcargo.AirportCode;
import be.kdg.airfreight.loadplanning.domain.flightcargo.CheckSeverity;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargo;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FloorPlan;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadCheck;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadCheckResult;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadPosition;
import be.kdg.airfreight.loadplanning.domain.flightcargo.Route;
import be.kdg.airfreight.loadplanning.domain.flightcargo.Uld;

/** Every ULD for the flight's destination must be unloadable without moving a ULD for another destination. */
public final class OffloadingCheck implements LoadCheck {
    private static final String NAME = "Offloading";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public CheckSeverity severity() {
        return CheckSeverity.HARD;
    }

    @Override
    public LoadCheckResult check(FlightCargo cargo, FloorPlan floorPlan) {
        AirportCode arrivalAirport = cargo.getFlightInfo().arrivalAirport();
        Map<LoadPosition, Uld> load = new HashMap<>();
        for (Uld uld : cargo.getLoad()) {
            load.put(positionOf(uld), uld);
        }

        Set<Uld> unloadable = new HashSet<>();
        boolean foundMore;
        do {
            foundMore = false;
            for (Uld uld : load.values()) {
                if (uld.getDestination().equals(arrivalAirport) && !unloadable.contains(uld)
                    && hasClearRoute(uld, load, unloadable, floorPlan)) {
                    unloadable.add(uld);
                    foundMore = true;
                }
            }
        } while (foundMore);

        List<String> stuck = load.values().stream()
            .filter(uld -> uld.getDestination().equals(arrivalAirport) && !unloadable.contains(uld))
            .sorted(Comparator.comparing((Uld uld) -> uld.getCode().value()))
            .map(uld -> describe(uld, load, unloadable, floorPlan))
            .toList();
        if (stuck.isEmpty()) {
            return LoadCheckResult.pass(NAME, severity());
        }
        return LoadCheckResult.fail(NAME, severity(),
            "Cannot be unloaded without moving other cargo: " + String.join("; ", stuck));
    }

    private static boolean hasClearRoute(Uld uld, Map<LoadPosition, Uld> load, Set<Uld> unloadable,
                                         FloorPlan floorPlan) {
        return floorPlan.routesToDoor(positionOf(uld)).stream()
            .anyMatch(route -> blockedPositions(route, load, unloadable).isEmpty());
    }

    /** The positions on a route that hold a ULD which cannot be taken out first. */
    private static List<LoadPosition> blockedPositions(Route route, Map<LoadPosition, Uld> load,
                                                       Set<Uld> unloadable) {
        return route.positions().stream()
            .filter(position -> {
                Uld occupant = load.get(position);
                return occupant != null && !unloadable.contains(occupant);
            })
            .toList();
    }

    private static String describe(Uld uld, Map<LoadPosition, Uld> load, Set<Uld> unloadable,
                                   FloorPlan floorPlan) {
        String blockedAt = floorPlan.routesToDoor(positionOf(uld)).stream()
            .flatMap(route -> blockedPositions(route, load, unloadable).stream())
            .map(LoadPosition::name)
            .distinct()
            .sorted()
            .collect(Collectors.joining(", "));
        return uld.getCode().value() + " at " + positionOf(uld).name() + " (blocked at " + blockedAt + ")";
    }

    private static LoadPosition positionOf(Uld uld) {
        return Objects.requireNonNull(uld.getPosition(), "A loaded ULD has a position");
    }
}
