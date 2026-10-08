package be.kdg.airfreight.flightoperations.infrastructure.flightplanner;

import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** The flight planners of the first version: the id is the id of the Keycloak account. */
@ConfigurationProperties("flight-operations")
record SeedProperties(@Nullable List<Planner> planners) {
    record Planner(UUID id, String name) {
    }
}
