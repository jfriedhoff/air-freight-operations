package be.kdg.airfreight.messaging.domain.flight;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import be.kdg.airfreight.messaging.domain.AircraftRegistration;
import be.kdg.airfreight.messaging.domain.FlightId;
import be.kdg.airfreight.messaging.domain.FormatVersion;
import be.kdg.airfreight.messaging.domain.MessageId;
import org.jmolecules.ddd.annotation.ValueObject;

/** The ETD changed: what an MVT-ED needs. */
@ValueObject
public record DepartureEstimateChanged(MessageId id, FormatVersion version, FlightId flightId, UUID flightReference,
                                       AircraftRegistration registration, Instant etd,
                                       Instant occurredOn) implements FlightEvent {
    public DepartureEstimateChanged {
        Objects.requireNonNull(id, "Id cannot be null");
        Objects.requireNonNull(version, "Version cannot be null");
        Objects.requireNonNull(flightId, "Flight id cannot be null");
        Objects.requireNonNull(flightReference, "Flight reference cannot be null");
        Objects.requireNonNull(registration, "Registration cannot be null");
        Objects.requireNonNull(etd, "ETD cannot be null");
        Objects.requireNonNull(occurredOn, "OccurredOn cannot be null");
    }
}
