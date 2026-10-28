package be.kdg.airfreight.flightoperations.api.flight;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** What a flight planner changes in a delay. The kind and the delay reason stay what they were entered as. */
public record ChangeDelayDto(@NotNull(message = "is required") Integer minutes,
                             @NotBlank(message = "is required") String description) {
}
