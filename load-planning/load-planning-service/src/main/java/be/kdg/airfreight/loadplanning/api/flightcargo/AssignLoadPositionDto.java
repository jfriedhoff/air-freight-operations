package be.kdg.airfreight.loadplanning.api.flightcargo;

import jakarta.validation.constraints.NotBlank;

/** What a load planner chooses: the name of a load position. Who chooses is in the login, never in the request. */
public record AssignLoadPositionDto(@NotBlank(message = "is required") String loadPosition) {
}
