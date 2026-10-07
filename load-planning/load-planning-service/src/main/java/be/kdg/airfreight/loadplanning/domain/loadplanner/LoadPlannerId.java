package be.kdg.airfreight.loadplanning.domain.loadplanner;

import java.util.Objects;
import java.util.UUID;

import be.kdg.airfreight.loadplanning.domain.NotFoundException;
import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
public record LoadPlannerId(UUID id) {
    public LoadPlannerId {
        Objects.requireNonNull(id, "Id cannot be null");
    }

    public static LoadPlannerId create() {
        return new LoadPlannerId(UUID.randomUUID());
    }

    public NotFoundException notFound() {
        return new NotFoundException("Load planner [" + id + "] not found");
    }
}
