package be.kdg.airfreight.loadplanning.domain.flightcargo;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.jmolecules.ddd.annotation.ValueObject;

/** The load positions of one aircraft type and, per position, the routes to the cargo door. */
@ValueObject
public record FloorPlan(AircraftType aircraftType, Map<LoadPosition, List<Route>> routesByPosition) {
    public FloorPlan {
        Objects.requireNonNull(aircraftType, "Aircraft type cannot be null");
        Objects.requireNonNull(routesByPosition, "Routes cannot be null");
        routesByPosition = Map.copyOf(routesByPosition);
    }

    public boolean contains(LoadPosition position) {
        return routesByPosition.containsKey(position);
    }

    public List<Route> routesToDoor(LoadPosition from) {
        return routesByPosition.getOrDefault(from, List.of());
    }

    public Optional<LoadPosition> positionNamed(String name) {
        return routesByPosition.keySet().stream()
            .filter(position -> position.name().equals(name))
            .findFirst();
    }
}
