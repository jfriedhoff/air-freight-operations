package be.kdg.airfreight.flightoperations.domain.flightplanner;

import java.util.Objects;
import java.util.UUID;

import be.kdg.airfreight.flightoperations.domain.NotFoundException;
import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
public record FlightPlannerId(UUID id) {
    public FlightPlannerId {
        Objects.requireNonNull(id, "Id cannot be null");
    }

    public static FlightPlannerId create() {
        return new FlightPlannerId(UUID.randomUUID());
    }

    public NotFoundException notFound() {
        return new NotFoundException("Flight planner [" + id + "] not found");
    }
}
