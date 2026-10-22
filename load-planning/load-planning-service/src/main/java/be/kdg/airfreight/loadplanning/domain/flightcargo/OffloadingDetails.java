package be.kdg.airfreight.loadplanning.domain.flightcargo;

import java.util.List;
import java.util.Objects;

import org.jmolecules.ddd.annotation.ValueObject;

/** Every ULD that the offloading check found stuck, with what is in its way. */
@ValueObject
public record OffloadingDetails(List<Obstruction> obstructions) implements LoadCheckDetails {
    public OffloadingDetails {
        Objects.requireNonNull(obstructions, "Obstructions cannot be null");
        obstructions = List.copyOf(obstructions);
    }
}
