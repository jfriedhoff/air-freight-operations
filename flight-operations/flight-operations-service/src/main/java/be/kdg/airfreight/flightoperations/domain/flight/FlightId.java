package be.kdg.airfreight.flightoperations.domain.flight;

import java.util.Objects;
import java.util.UUID;

import be.kdg.airfreight.flightoperations.domain.NotFoundException;
import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
public record FlightId(UUID id) {
    public FlightId {
        Objects.requireNonNull(id, "Id cannot be null");
    }

    public static FlightId create() {
        return new FlightId(UUID.randomUUID());
    }

    public NotFoundException notFound() {
        return new NotFoundException("Flight [" + id + "] not found");
    }
}
