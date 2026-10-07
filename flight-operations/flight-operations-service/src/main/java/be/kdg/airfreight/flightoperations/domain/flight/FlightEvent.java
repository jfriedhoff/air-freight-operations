package be.kdg.airfreight.flightoperations.domain.flight;

import java.time.Instant;

public sealed interface FlightEvent
    permits FlightAnnounced, DepartureEstimateChanged, ArrivalEstimateChanged,
            FinalizedLoadReceived, FlightDeparted, FlightLanded, FlightRemoved {
    FlightId flightId();

    Instant occurredOn();
}
