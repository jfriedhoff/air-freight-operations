package be.kdg.airfreight.messaging.domain.flight;

import java.time.Instant;
import java.util.UUID;

import be.kdg.airfreight.messaging.domain.FlightId;
import be.kdg.airfreight.messaging.domain.receivedmessage.IncomingMessage;

/**
 * Something that happened to a flight in Flight Operations. Each kind of event is its own record and
 * carries only what changed, as Flight Operations publishes it. Their order matters: they are read
 * from one queue by one consumer.
 */
public sealed interface FlightEvent extends IncomingMessage
    permits FlightAnnounced, DepartureEstimateChanged, ArrivalEstimateChanged, FlightDeparted, FlightLanded,
            FlightRemoved {
    FlightId flightId();

    /** The id Flight Operations gave the flight. Passed on, not used here. */
    UUID flightReference();

    Instant occurredOn();
}
