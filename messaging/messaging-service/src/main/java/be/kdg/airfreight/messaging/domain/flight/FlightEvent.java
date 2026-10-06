package be.kdg.airfreight.messaging.domain.flight;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import be.kdg.airfreight.messaging.domain.AircraftRegistration;
import be.kdg.airfreight.messaging.domain.AirportCode;
import be.kdg.airfreight.messaging.domain.FlightId;
import be.kdg.airfreight.messaging.domain.FormatVersion;
import be.kdg.airfreight.messaging.domain.MessageId;
import be.kdg.airfreight.messaging.domain.receivedmessage.IncomingMessage;
import org.jmolecules.ddd.annotation.ValueObject;
import org.jspecify.annotations.Nullable;

/**
 * Something that happened to a flight in Flight Operations. It carries the whole flight, so any
 * partner message can be written from it and it can be passed on to Load Planning as it is.
 */
@ValueObject
public record FlightEvent(MessageId id, FormatVersion version, FlightEventKind kind, FlightId flightId,
                          UUID flightReference, AirportCode arrivalAirport, AircraftRegistration registration,
                          Instant std, Instant sta, Instant etd, Instant eta, @Nullable Instant atd,
                          @Nullable Instant ata, long flightVersion, Instant occurredOn) implements IncomingMessage {
    public FlightEvent {
        Objects.requireNonNull(id, "Id cannot be null");
        Objects.requireNonNull(version, "Version cannot be null");
        Objects.requireNonNull(kind, "Kind cannot be null");
        Objects.requireNonNull(flightId, "Flight id cannot be null");
        Objects.requireNonNull(flightReference, "Flight reference cannot be null");
        Objects.requireNonNull(arrivalAirport, "Arrival airport cannot be null");
        Objects.requireNonNull(registration, "Registration cannot be null");
        Objects.requireNonNull(std, "STD cannot be null");
        Objects.requireNonNull(sta, "STA cannot be null");
        Objects.requireNonNull(etd, "ETD cannot be null");
        Objects.requireNonNull(eta, "ETA cannot be null");
        Objects.requireNonNull(occurredOn, "OccurredOn cannot be null");
    }
}
