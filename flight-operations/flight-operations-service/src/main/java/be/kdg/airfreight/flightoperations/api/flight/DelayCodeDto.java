package be.kdg.airfreight.flightoperations.api.flight;

import be.kdg.airfreight.flightoperations.domain.flight.DelayCode;
import be.kdg.airfreight.flightoperations.domain.flight.DelayCodeCatalog;
import be.kdg.airfreight.flightoperations.domain.flight.DelayReason;
import org.jspecify.annotations.Nullable;

public record DelayCodeDto(String code, @Nullable String description) {
    public static DelayCodeDto from(DelayReason reason) {
        return new DelayCodeDto(reason.code().value(), reason.description());
    }

    public static DelayCodeDto from(DelayCode code, DelayCodeCatalog delayCodes) {
        return new DelayCodeDto(code.value(), delayCodes.find(code).map(DelayReason::description).orElse(null));
    }
}
