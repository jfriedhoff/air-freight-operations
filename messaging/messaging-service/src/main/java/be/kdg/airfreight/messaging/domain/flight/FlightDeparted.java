package be.kdg.airfreight.messaging.domain.flight;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import be.kdg.airfreight.messaging.domain.AircraftRegistration;
import be.kdg.airfreight.messaging.domain.FlightId;
import be.kdg.airfreight.messaging.domain.FormatVersion;
import be.kdg.airfreight.messaging.domain.MessageId;
import org.jmolecules.ddd.annotation.ValueObject;

/** The departure was registered: what an MVT-AD needs. */
@ValueObject
public record FlightDeparted(MessageId id, FormatVersion version, FlightId flightId, UUID flightReference,
                             AircraftRegistration registration, Instant atd,
                             Instant occurredOn) implements FlightEvent {
    public FlightDeparted {
        Objects.requireNonNull(id, "Id cannot be null");
        Objects.requireNonNull(version, "Version cannot be null");
        Objects.requireNonNull(flightId, "Flight id cannot be null");
        Objects.requireNonNull(flightReference, "Flight reference cannot be null");
        Objects.requireNonNull(registration, "Registration cannot be null");
        Objects.requireNonNull(atd, "ATD cannot be null");
        Objects.requireNonNull(occurredOn, "OccurredOn cannot be null");
    }
}
