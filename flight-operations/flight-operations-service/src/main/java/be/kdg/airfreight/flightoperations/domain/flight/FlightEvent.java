package be.kdg.airfreight.flightoperations.domain.flight;

import java.time.Instant;

public sealed interface FlightEvent
    permits FlightAnnounced, DepartureEstimateChanged, ArrivalEstimateChanged,
            FinalizedLoadReceived, FlightDeparted, FlightLanded {
    FlightId flightId();

    Instant occurredOn();
}
