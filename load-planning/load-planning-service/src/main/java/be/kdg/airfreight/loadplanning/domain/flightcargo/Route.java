package be.kdg.airfreight.loadplanning.domain.flightcargo;

import java.util.List;
import java.util.Objects;

import org.jmolecules.ddd.annotation.ValueObject;

/** One way to the cargo door: the positions that must be clear to take it. */
@ValueObject
public record Route(List<LoadPosition> positions) {
    public Route {
        Objects.requireNonNull(positions, "Positions cannot be null");
        positions = List.copyOf(positions);
    }
}
