package be.kdg.airfreight.messaging.domain.load;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import be.kdg.airfreight.messaging.domain.FlightId;
import be.kdg.airfreight.messaging.domain.FormatVersion;
import be.kdg.airfreight.messaging.domain.MessageId;
import be.kdg.airfreight.messaging.domain.receivedmessage.IncomingMessage;
import org.jmolecules.ddd.annotation.ValueObject;

/** The load of a flight as Load Planning finalized it: the positioned ULDs. */
@ValueObject
public record LoadFinalized(MessageId id, FormatVersion version, FlightId flightId, List<CpmLine> load,
                            Instant occurredOn) implements IncomingMessage {
    public LoadFinalized {
        Objects.requireNonNull(id, "Id cannot be null");
        Objects.requireNonNull(version, "Version cannot be null");
        Objects.requireNonNull(flightId, "Flight id cannot be null");
        Objects.requireNonNull(load, "Load cannot be null");
        Objects.requireNonNull(occurredOn, "OccurredOn cannot be null");
        load = List.copyOf(load);
    }

    public CpmCreated cpmCreated(Instant now) {
        return new CpmCreated(flightId, now);
    }
}
