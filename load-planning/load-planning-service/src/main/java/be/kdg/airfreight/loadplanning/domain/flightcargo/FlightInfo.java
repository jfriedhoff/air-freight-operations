package be.kdg.airfreight.loadplanning.domain.flightcargo;

import java.time.Instant;
import java.util.Objects;

import org.jmolecules.ddd.annotation.ValueObject;

/**
 * Read-only copy of the flight, built from the flight events. Each event changes one part of it;
 * the events arrive in order, so the last one to arrive is the newest.
 */
@ValueObject
public record FlightInfo(AirportCode arrivalAirport, AircraftType aircraftType, Instant std, Instant sta,
                         Instant etd, Instant eta, boolean departed) {
    public FlightInfo {
        Objects.requireNonNull(arrivalAirport, "Arrival airport cannot be null");
        Objects.requireNonNull(aircraftType, "Aircraft type cannot be null");
        Objects.requireNonNull(std, "STD cannot be null");
        Objects.requireNonNull(sta, "STA cannot be null");
        Objects.requireNonNull(etd, "ETD cannot be null");
        Objects.requireNonNull(eta, "ETA cannot be null");
    }

    /** A flight as it is announced: no delays yet, so the estimated times are the scheduled ones. */
    public static FlightInfo scheduled(AirportCode arrivalAirport, AircraftType aircraftType, Instant std,
                                       Instant sta) {
        return new FlightInfo(arrivalAirport, aircraftType, std, sta, std, sta, false);
    }

    public FlightInfo withEstimatedDeparture(Instant newEtd) {
        return new FlightInfo(arrivalAirport, aircraftType, std, sta, newEtd, eta, departed);
    }

    public FlightInfo withEstimatedArrival(Instant newEta) {
        return new FlightInfo(arrivalAirport, aircraftType, std, sta, etd, newEta, departed);
    }

    public FlightInfo asDeparted() {
        return new FlightInfo(arrivalAirport, aircraftType, std, sta, etd, eta, true);
    }
}
