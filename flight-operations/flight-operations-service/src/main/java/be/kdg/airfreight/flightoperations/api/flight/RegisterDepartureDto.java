package be.kdg.airfreight.flightoperations.api.flight;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/** The ATD as a UTC moment. The page converts the Brussels time that the flight planner typed. */
public record RegisterDepartureDto(@NotNull(message = "is required") Instant atd) {
}
