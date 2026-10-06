package be.kdg.airfreight.loadplanning.domain.flightcargo;

import java.time.Instant;

public sealed interface FlightCargoEvent
    permits UldBooked, LoadPositionAssigned, LoadPositionCleared, LoadFinalized {
    FlightCargoId flightCargoId();

    Instant occurredOn();
}
