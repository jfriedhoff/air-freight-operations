package be.kdg.airfreight.loadplanning.domain.flightcargo.check;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
        return LoadCheckResult.fail(NAME, severity(), String.join("; ", stuck));
    }

    private static boolean hasClearRoute(Uld uld, Map<LoadPosition, Uld> load, Set<Uld> unloadable,
                                         FloorPlan floorPlan) {
        return floorPlan.routesToDoor(positionOf(uld)).stream()
            .anyMatch(route -> blockers(route, load, unloadable).isEmpty());
    }

    /** The ULDs on a route that cannot be taken out first. */
    private static List<Uld> blockers(Route route, Map<LoadPosition, Uld> load, Set<Uld> unloadable) {
        return route.positions().stream()
            .map(load::get)
            .filter(occupant -> occupant != null && !unloadable.contains(occupant))
            .toList();
    }

    /** "AKE16689R7 on ML cannot be offloaded because AKE20417HJ on PL is in the way". */
    private static String describe(Uld uld, Map<LoadPosition, Uld> load, Set<Uld> unloadable,
                                   FloorPlan floorPlan) {
        List<String> inTheWay = floorPlan.routesToDoor(positionOf(uld)).stream()
            .flatMap(route -> blockers(route, load, unloadable).stream())
            .distinct()
            .map(OffloadingCheck::where)
            .sorted()
            .toList();
        return where(uld) + " cannot be offloaded because "
            + listed(inTheWay) + (inTheWay.size() == 1 ? " is" : " are") + " in the way";
    }

    /** "a", "a and b", "a, b and c". */
    private static String listed(List<String> items) {
        if (items.size() == 1) {
            return items.getFirst();
        }
        return String.join(", ", items.subList(0, items.size() - 1)) + " and " + items.getLast();
    }

    private static String where(Uld uld) {
        return uld.getCode().value() + " on " + positionOf(uld).name();
    }

    private static LoadPosition positionOf(Uld uld) {
        return Objects.requireNonNull(uld.getPosition(), "A loaded ULD has a position");
    }
}
