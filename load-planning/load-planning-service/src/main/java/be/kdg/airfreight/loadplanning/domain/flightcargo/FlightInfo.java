package be.kdg.airfreight.loadplanning.domain.flightcargo;

import java.time.Instant;
import java.util.Objects;

import org.jmolecules.ddd.annotation.ValueObject;

/** Read-only copy of the flight, built from a flight event and replaced as a whole. */
@ValueObject
public record FlightInfo(AirportCode arrivalAirport, AircraftType aircraftType, Instant std, Instant sta,
                         Instant etd, Instant eta, boolean departed, long version) {
    public FlightInfo {
        Objects.requireNonNull(arrivalAirport, "Arrival airport cannot be null");
        Objects.requireNonNull(aircraftType, "Aircraft type cannot be null");
        Objects.requireNonNull(std, "STD cannot be null");
        Objects.requireNonNull(sta, "STA cannot be null");
        Objects.requireNonNull(etd, "ETD cannot be null");
        Objects.requireNonNull(eta, "ETA cannot be null");
    }
}
