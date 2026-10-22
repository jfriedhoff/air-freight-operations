package be.kdg.airfreight.loadplanning.domain.flightcargo;

import org.jmolecules.ddd.annotation.ValueObject;

/** A ULD and where it stands: a line of the finalized load, or a ULD that the offloading check names. */
@ValueObject
public record LoadedUld(UldCode code, LoadPosition position, Weight weight, AirportCode destination) {
}
