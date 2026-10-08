package be.kdg.airfreight.flightoperations.domain.flight;

import java.util.Optional;

/** The IATA airport list: the name that goes with a code, if the list has it. */
public interface AirportCatalog {
    Optional<String> nameOf(AirportCode code);
}
