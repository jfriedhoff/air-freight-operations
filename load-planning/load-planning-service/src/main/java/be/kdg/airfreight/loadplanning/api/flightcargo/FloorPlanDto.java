package be.kdg.airfreight.loadplanning.api.flightcargo;

import java.util.List;

import be.kdg.airfreight.loadplanning.domain.flightcargo.FloorPlan;
import be.kdg.airfreight.loadplanning.domain.flightcargo.Side;
import org.jspecify.annotations.Nullable;

/**
 * The floor plan as the cargo page draws it: the load positions in deck order (nose to tail) with their side, and
 * the position at the cargo door. A position in the nose or the tail has no side. The page draws a second aircraft
 * type from this without a change of its own.
 */
public record FloorPlanDto(List<LoadPositionDto> positions, String door) {
    public record LoadPositionDto(String name, @Nullable Side side) {
    }

    public static FloorPlanDto from(FloorPlan floorPlan) {
        return new FloorPlanDto(floorPlan.positions().stream()
            .map(position -> new LoadPositionDto(position.name(), position.side()))
            .toList(), floorPlan.door().name());
    }
}
