package be.kdg.airfreight.flightoperations.api.flight;

import be.kdg.airfreight.flightoperations.domain.flight.DelayKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** What a flight planner enters. No id and no times: estimated times are derived, never entered. */
public record AddDelayDto(@NotNull(message = "is required") DelayKind kind,
                          @NotNull(message = "is required") Integer minutes,
                          @NotBlank(message = "is required") String reason,
                          @NotBlank(message = "is required") String description) {
}
