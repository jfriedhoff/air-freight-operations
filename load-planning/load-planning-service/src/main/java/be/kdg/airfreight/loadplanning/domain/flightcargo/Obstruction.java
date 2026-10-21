package be.kdg.airfreight.loadplanning.domain.flightcargo;

import java.util.List;
import java.util.Objects;

import org.jmolecules.ddd.annotation.ValueObject;

/** A ULD for the flight's destination that cannot be offloaded, and the ULDs in its way on its routes to the door. */
@ValueObject
public record Obstruction(LoadedUld stuck, List<LoadedUld> inTheWay) {
    public Obstruction {
        Objects.requireNonNull(stuck, "Stuck ULD cannot be null");
        Objects.requireNonNull(inTheWay, "ULDs in the way cannot be null");
        inTheWay = List.copyOf(inTheWay);
    }
}
