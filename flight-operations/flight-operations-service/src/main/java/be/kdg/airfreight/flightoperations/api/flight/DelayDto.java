package be.kdg.airfreight.flightoperations.api.flight;

import java.util.Map;
import java.util.UUID;

import be.kdg.airfreight.flightoperations.domain.flight.Delay;
import be.kdg.airfreight.flightoperations.domain.flight.DelayCodeCatalog;
import be.kdg.airfreight.flightoperations.domain.flight.DelayKind;
import be.kdg.airfreight.flightoperations.domain.flightplanner.FlightPlannerId;
import org.jspecify.annotations.Nullable;

/** A delay of a flight. lastChangedBy is the name of the flight planner who entered or last changed it. */
public record DelayDto(UUID id, DelayKind kind, int minutes, DelayCodeDto reason, String description,
                       @Nullable String lastChangedBy) {
    public static DelayDto from(Delay delay, DelayCodeCatalog delayCodes, Map<FlightPlannerId, String> plannerNames) {
        return new DelayDto(delay.getId().id(), delay.getKind(), delay.getMinutes().minutes(),
            DelayCodeDto.from(delay.getReason(), delayCodes), delay.getDescription(),
            plannerNames.get(delay.getLastChangedBy()));
    }
}
