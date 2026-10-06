package be.kdg.airfreight.flightoperations.domain.flight;

import java.util.Objects;
import java.util.UUID;

import be.kdg.airfreight.flightoperations.domain.NotFoundException;
import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
public record DelayId(UUID id) {
    public DelayId {
        Objects.requireNonNull(id, "Id cannot be null");
    }

    public static DelayId create() {
        return new DelayId(UUID.randomUUID());
    }

    public NotFoundException notFound() {
        return new NotFoundException("Delay [" + id + "] not found");
    }
}
