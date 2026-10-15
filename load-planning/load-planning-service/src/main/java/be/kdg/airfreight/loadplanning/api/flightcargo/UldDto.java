package be.kdg.airfreight.loadplanning.api.flightcargo;

import java.util.Map;

import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadPosition;
import be.kdg.airfreight.loadplanning.domain.flightcargo.Uld;
import be.kdg.airfreight.loadplanning.domain.loadplanner.LoadPlannerId;
import org.jspecify.annotations.Nullable;

/**
 * A booked ULD. The load position is the name of its place on the deck, or nothing when it has none.
 * positionChangedBy is the name of the load planner who last put it on a load position or took it off.
 */
public record UldDto(String code, WeightDto weight, String destination, @Nullable String loadPosition,
                     @Nullable String positionChangedBy) {
    public static UldDto from(Uld uld, Map<LoadPlannerId, String> plannerNames) {
        LoadPosition position = uld.getPosition();
        LoadPlannerId changedBy = uld.getPositionChangedBy();
        return new UldDto(uld.getCode().value(), WeightDto.from(uld.getWeight()), uld.getDestination().value(),
            position == null ? null : position.name(), changedBy == null ? null : plannerNames.get(changedBy));
    }
}
