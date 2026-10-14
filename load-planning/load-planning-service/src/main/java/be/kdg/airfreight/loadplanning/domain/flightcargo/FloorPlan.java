package be.kdg.airfreight.loadplanning.domain.flightcargo;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import be.kdg.airfreight.loadplanning.domain.InvalidValueException;
import org.jmolecules.ddd.annotation.ValueObject;

/** The load positions of one aircraft type and, per position, the routes to the cargo door. */
@ValueObject
public record FloorPlan(AircraftType aircraftType, Map<LoadPosition, List<Route>> routesByPosition) {
    public FloorPlan {
        Objects.requireNonNull(aircraftType, "Aircraft type cannot be null");
        Objects.requireNonNull(routesByPosition, "Routes cannot be null");
        // keeps the order in which the positions were added: nose to tail
        routesByPosition = Collections.unmodifiableMap(new LinkedHashMap<>(routesByPosition));
    }

    public boolean contains(LoadPosition position) {
        return routesByPosition.containsKey(position);
    }

    public List<Route> routesToDoor(LoadPosition from) {
        return routesByPosition.getOrDefault(from, List.of());
    }

    /** The load positions in the order of the floor plan. */
    public List<LoadPosition> positions() {
        return List.copyOf(routesByPosition.keySet());
    }

    /** The position with this name; a name that is not on this floor plan is an invalid value. */
    public LoadPosition position(String name) {
        return positionNamed(name).orElseThrow(() -> notOnTheFloorPlan(name));
    }

    public InvalidValueException notOnTheFloorPlan(String name) {
        return new InvalidValueException("Load position " + name + " is not on the floor plan of aircraft type "
            + aircraftType.code());
    }

    public Optional<LoadPosition> positionNamed(String name) {
        return routesByPosition.keySet().stream()
            .filter(position -> position.name().equals(name))
            .findFirst();
    }
}
