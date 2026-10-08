package be.kdg.airfreight.loadplanning.infrastructure.floorplan;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import be.kdg.airfreight.loadplanning.domain.flightcargo.AircraftType;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FloorPlan;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadPosition;
import be.kdg.airfreight.loadplanning.domain.flightcargo.Route;
import be.kdg.airfreight.loadplanning.domain.flightcargo.Side;

/** Main deck of the Boeing 747-400F, seen from above with the nose to the left. Cargo door on the left at row R. */
public final class Boeing747400FFloorPlan {
    public static final AircraftType AIRCRAFT_TYPE = new AircraftType("B747-400F");

    private static final List<String> NOSE = List.of("A1", "A2", "B1");
    private static final List<String> ROWS =
        List.of("C", "D", "E", "F", "G", "H", "J", "K", "L", "M", "P", "R", "S", "T", "U");
    private static final String TAIL = "X";
    private static final String DOOR_ROW = "R";

    private Boeing747400FFloorPlan() {
    }

    public static FloorPlan build() {
        Map<LoadPosition, List<Route>> routes = new LinkedHashMap<>();
        for (String name : NOSE) {
            routes.put(new LoadPosition(name, null), routesViaRow(nosePositionsToPass(name), ROWS.getFirst()));
        }
        for (String row : ROWS) {
            routes.put(inRow(row, Side.LEFT), List.of(new Route(pathToDoor(row, Side.LEFT))));
            routes.put(inRow(row, Side.RIGHT), List.of(new Route(pathToDoor(row, Side.RIGHT))));
        }
        routes.put(new LoadPosition(TAIL, null), routesViaRow(List.of(), ROWS.getLast()));
        return new FloorPlan(AIRCRAFT_TYPE, routes);
    }

    /** The nose positions between this one and row C: A1 passes A2 and B1, A2 passes B1. */
    private static List<LoadPosition> nosePositionsToPass(String name) {
        return NOSE.subList(NOSE.indexOf(name) + 1, NOSE.size()).stream()
            .map(passed -> new LoadPosition(passed, null))
            .toList();
    }

    /**
     * From the nose or the tail: first the positions to pass, then both positions of the neighbouring row must be
     * clear, then left or right.
     */
    private static List<Route> routesViaRow(List<LoadPosition> toPass, String row) {
        List<LoadPosition> manoeuvre = joined(toPass, List.of(inRow(row, Side.LEFT), inRow(row, Side.RIGHT)));
        return List.of(
            new Route(joined(manoeuvre, pathToDoor(row, Side.LEFT))),
            new Route(joined(manoeuvre, pathToDoor(row, Side.RIGHT))));
    }

    /** The positions between a row and the door on one side; the right side ends by crossing to the door. */
    private static List<LoadPosition> pathToDoor(String row, Side side) {
        int from = ROWS.indexOf(row);
        int door = ROWS.indexOf(DOOR_ROW);
        int step = Integer.compare(door, from);
        List<LoadPosition> path = new ArrayList<>();
        for (int i = from + step; step != 0 && i != door + step; i += step) {
            path.add(inRow(ROWS.get(i), side));
        }
        if (side == Side.RIGHT) {
            path.add(inRow(DOOR_ROW, Side.LEFT));
        }
        return path;
    }

    private static LoadPosition inRow(String row, Side side) {
        return new LoadPosition(row + (side == Side.LEFT ? "L" : "R"), side);
    }

    private static List<LoadPosition> joined(List<LoadPosition> first, List<LoadPosition> second) {
        List<LoadPosition> all = new ArrayList<>(first);
        all.addAll(second);
        return all;
    }
}
