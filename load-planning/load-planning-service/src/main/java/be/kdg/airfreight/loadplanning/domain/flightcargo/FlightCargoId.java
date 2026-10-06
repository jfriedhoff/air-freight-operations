package be.kdg.airfreight.loadplanning.domain.flightcargo;

import java.util.Objects;
import java.util.UUID;

import be.kdg.airfreight.loadplanning.domain.NotFoundException;
import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
public record FlightCargoId(UUID id) {
    public FlightCargoId {
        Objects.requireNonNull(id, "Id cannot be null");
    }

    public static FlightCargoId create() {
        return new FlightCargoId(UUID.randomUUID());
    }

    public NotFoundException notFound() {
        return new NotFoundException("Flight cargo [" + id + "] not found");
    }
}
