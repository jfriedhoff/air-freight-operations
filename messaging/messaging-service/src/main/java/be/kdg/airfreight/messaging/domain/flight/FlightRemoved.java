package be.kdg.airfreight.messaging.domain.flight;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import be.kdg.airfreight.messaging.domain.FlightId;
import be.kdg.airfreight.messaging.domain.FormatVersion;
import be.kdg.airfreight.messaging.domain.MessageId;
import org.jmolecules.ddd.annotation.ValueObject;

/** The flight was deleted after its retention period: every message about it goes too. */
@ValueObject
public record FlightRemoved(MessageId id, FormatVersion version, FlightId flightId, UUID flightReference,
                            Instant occurredOn) implements FlightEvent {
    public FlightRemoved {
        Objects.requireNonNull(id, "Id cannot be null");
        Objects.requireNonNull(version, "Version cannot be null");
        Objects.requireNonNull(flightId, "Flight id cannot be null");
        Objects.requireNonNull(flightReference, "Flight reference cannot be null");
        Objects.requireNonNull(occurredOn, "OccurredOn cannot be null");
    }
}
