package be.kdg.airfreight.messaging.domain.flight;

public enum FlightEventKind {
    ANNOUNCED,
    DEPARTURE_ESTIMATE_CHANGED,
    ARRIVAL_ESTIMATE_CHANGED,
    DEPARTED,
    LANDED,
    REMOVED
}
