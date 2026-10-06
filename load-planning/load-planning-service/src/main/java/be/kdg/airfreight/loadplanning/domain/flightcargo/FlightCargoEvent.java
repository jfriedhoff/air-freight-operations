package be.kdg.airfreight.loadplanning.domain.flightcargo;

import java.time.Instant;

public sealed interface FlightCargoEvent permits UldBooked, LoadPositionAssigned, LoadPositionCleared {
    FlightCargoId flightCargoId();

    Instant occurredOn();
}
