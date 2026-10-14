package be.kdg.airfreight.loadplanning.infrastructure.loadplanner;

import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** The load planners of the first version: the id is the id of the Keycloak account. */
@ConfigurationProperties("load-planning")
record SeedProperties(@Nullable List<Planner> planners) {
    record Planner(UUID id, String name) {
    }
}
