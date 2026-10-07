package be.kdg.airfreight.messaging.domain.load;

import java.time.Instant;

import be.kdg.airfreight.messaging.domain.FlightId;
import org.jmolecules.event.annotation.DomainEvent;

/** The CPM of a flight has been written: Flight Operations may now register the departure. */
@DomainEvent
public record CpmCreated(FlightId flightId, Instant occurredOn) {
}
