package be.kdg.airfreight.messaging.domain.flight;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import be.kdg.airfreight.messaging.domain.AircraftRegistration;
import be.kdg.airfreight.messaging.domain.AirportCode;
import be.kdg.airfreight.messaging.domain.FlightId;
import be.kdg.airfreight.messaging.domain.FormatVersion;
import be.kdg.airfreight.messaging.domain.MessageId;
import org.jmolecules.ddd.annotation.ValueObject;

/** The ETA changed: what an MVT-EA needs. */
@ValueObject
public record ArrivalEstimateChanged(MessageId id, FormatVersion version, FlightId flightId, UUID flightReference,
                                     AirportCode arrivalAirport, AircraftRegistration registration, Instant eta,
                                     Instant occurredOn) implements FlightEvent {
    public ArrivalEstimateChanged {
        Objects.requireNonNull(id, "Id cannot be null");
        Objects.requireNonNull(version, "Version cannot be null");
        Objects.requireNonNull(flightId, "Flight id cannot be null");
        Objects.requireNonNull(flightReference, "Flight reference cannot be null");
        Objects.requireNonNull(arrivalAirport, "Arrival airport cannot be null");
        Objects.requireNonNull(registration, "Registration cannot be null");
        Objects.requireNonNull(eta, "ETA cannot be null");
        Objects.requireNonNull(occurredOn, "OccurredOn cannot be null");
    }
}
