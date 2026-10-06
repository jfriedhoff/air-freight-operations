package be.kdg.airfreight.loadplanning.domain.flightcargo;

import org.jmolecules.ddd.annotation.ValueObject;

/** One line of a finalized load: a ULD and where it stands. */
@ValueObject
public record LoadedUld(UldCode code, LoadPosition position, Weight weight, AirportCode destination) {
}
