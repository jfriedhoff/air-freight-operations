package be.kdg.airfreight.flightoperations.api.flight;

import java.util.UUID;

import be.kdg.airfreight.flightoperations.domain.flight.Delay;
import be.kdg.airfreight.flightoperations.domain.flight.DelayCodeCatalog;
import be.kdg.airfreight.flightoperations.domain.flight.DelayKind;

public record DelayDto(UUID id, DelayKind kind, int minutes, DelayCodeDto reason, String description) {
    public static DelayDto from(Delay delay, DelayCodeCatalog delayCodes) {
        return new DelayDto(delay.getId().id(), delay.getKind(), delay.getMinutes().minutes(),
            DelayCodeDto.from(delay.getReason(), delayCodes), delay.getDescription());
    }
}
