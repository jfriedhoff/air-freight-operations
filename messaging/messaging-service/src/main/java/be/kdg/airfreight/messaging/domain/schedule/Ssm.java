package be.kdg.airfreight.messaging.domain.schedule;

import java.util.Objects;

import be.kdg.airfreight.messaging.domain.FormatVersion;
import be.kdg.airfreight.messaging.domain.MessageId;
import be.kdg.airfreight.messaging.domain.receivedmessage.IncomingMessage;
import org.jmolecules.ddd.annotation.ValueObject;

/** A schedule message as read: which message it is, and the flight series it announces. */
@ValueObject
public record Ssm(MessageId id, FormatVersion version, FlightSeries series) implements IncomingMessage {
    public Ssm {
        Objects.requireNonNull(id, "Id cannot be null");
        Objects.requireNonNull(version, "Version cannot be null");
        Objects.requireNonNull(series, "Flight series cannot be null");
    }
}
