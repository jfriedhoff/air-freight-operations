package be.kdg.airfreight.loadplanning.api.flightcargo;

import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadPosition;
import be.kdg.airfreight.loadplanning.domain.flightcargo.Uld;
import org.jspecify.annotations.Nullable;

/** A booked ULD. The load position is the name of its place on the deck, or nothing when it has none. */
public record UldDto(String code, WeightDto weight, String destination, @Nullable String loadPosition) {
    public static UldDto from(Uld uld) {
        LoadPosition position = uld.getPosition();
        return new UldDto(uld.getCode().value(), WeightDto.from(uld.getWeight()), uld.getDestination().value(),
            position == null ? null : position.name());
    }
}
