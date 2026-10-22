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
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadedUld;
import be.kdg.airfreight.loadplanning.domain.flightcargo.Obstruction;
import be.kdg.airfreight.loadplanning.domain.flightcargo.OffloadingDetails;
import be.kdg.airfreight.loadplanning.domain.flightcargo.Route;
import be.kdg.airfreight.loadplanning.domain.flightcargo.Uld;

/** Every ULD for the flight's destination must be unloadable without moving a ULD for another destination. */
public final class OffloadingCheck implements LoadCheck {
    private static final String NAME = "Offloading";
    private static final Comparator<Uld> BY_CODE = Comparator.comparing(uld -> uld.getCode().value());

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

        // Found once: the sentence and the details are both made from these, so they cannot say different things.
        List<Obstruction> obstructions = load.values().stream()
            .filter(uld -> uld.getDestination().equals(arrivalAirport) && !unloadable.contains(uld))
            .sorted(BY_CODE)
            .map(uld -> obstructionOf(uld, load, unloadable, floorPlan))
            .toList();
        if (obstructions.isEmpty()) {
            return LoadCheckResult.pass(NAME, severity());
        }
        return LoadCheckResult.fail(NAME, severity(),
            obstructions.stream().map(OffloadingCheck::describe).collect(Collectors.joining("; ")),
            new OffloadingDetails(obstructions));
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

    /** The stuck ULD and every ULD in its way on any of its routes, in the order of their codes. */
    private static Obstruction obstructionOf(Uld uld, Map<LoadPosition, Uld> load, Set<Uld> unloadable,
                                             FloorPlan floorPlan) {
        List<LoadedUld> inTheWay = floorPlan.routesToDoor(positionOf(uld)).stream()
            .flatMap(route -> blockers(route, load, unloadable).stream())
            .distinct()
            .sorted(BY_CODE)
            .map(OffloadingCheck::loaded)
            .toList();
        return new Obstruction(loaded(uld), inTheWay);
    }

    /** "AKE16689R7 on ML cannot be offloaded because AKE20417HJ on PL is in the way". */
    private static String describe(Obstruction obstruction) {
        List<String> inTheWay = obstruction.inTheWay().stream().map(OffloadingCheck::where).toList();
        return where(obstruction.stuck()) + " cannot be offloaded because "
            + listed(inTheWay) + (inTheWay.size() == 1 ? " is" : " are") + " in the way";
    }

    /** "a", "a and b", "a, b and c". */
    private static String listed(List<String> items) {
        if (items.size() == 1) {
            return items.getFirst();
        }
        return String.join(", ", items.subList(0, items.size() - 1)) + " and " + items.getLast();
    }

    private static String where(LoadedUld uld) {
        return uld.code().value() + " on " + uld.position().name();
    }

    private static LoadedUld loaded(Uld uld) {
        return new LoadedUld(uld.getCode(), positionOf(uld), uld.getWeight(), uld.getDestination());
    }

    private static LoadPosition positionOf(Uld uld) {
        return Objects.requireNonNull(uld.getPosition(), "A loaded ULD has a position");
    }
}
