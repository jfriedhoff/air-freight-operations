package be.kdg.airfreight.messaging.domain.outgoingmessage;

import java.time.Instant;
import java.util.Objects;

import be.kdg.airfreight.messaging.domain.DomainRuleViolationException;
import be.kdg.airfreight.messaging.domain.FlightId;
import be.kdg.airfreight.messaging.domain.FormatVersion;
import be.kdg.airfreight.messaging.domain.MessageId;
import be.kdg.airfreight.messaging.domain.MessageType;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.jspecify.annotations.Nullable;

/** One message to the partners, from the moment it is written until it is sent. */
@AggregateRoot
public class OutgoingMessage {
    @Identity
    private final MessageId id;
    private final FlightId flightId;
    private final MessageType type;
    private final FormatVersion formatVersion;
    private final Instant createdAt;
    private final String body;
    private @Nullable Instant sentAt;

    private OutgoingMessage(MessageId id, FlightId flightId, MessageType type, FormatVersion formatVersion,
                            Instant createdAt, String body, @Nullable Instant sentAt) {
        this.id = Objects.requireNonNull(id, "Id cannot be null");
        this.flightId = Objects.requireNonNull(flightId, "Flight id cannot be null");
        this.type = Objects.requireNonNull(type, "Type cannot be null");
        this.formatVersion = Objects.requireNonNull(formatVersion, "Format version cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "Creation time cannot be null");
        this.body = Objects.requireNonNull(body, "Body cannot be null");
        this.sentAt = sentAt;
    }

    public static OutgoingMessage create(MessageId id, FlightId flightId, MessageType type,
                                         FormatVersion formatVersion, Instant createdAt, String body) {
        return new OutgoingMessage(id, flightId, type, formatVersion, createdAt, body, null);
    }

    public static OutgoingMessage hydrate(MessageId id, FlightId flightId, MessageType type,
                                          FormatVersion formatVersion, Instant createdAt, String body,
                                          @Nullable Instant sentAt) {
        return new OutgoingMessage(id, flightId, type, formatVersion, createdAt, body, sentAt);
    }

    public void markSent(Instant at) {
        if (sentAt != null) {
            throw new DomainRuleViolationException("Message [" + id.id() + "] has already been sent");
        }
        sentAt = Objects.requireNonNull(at, "Sending time cannot be null");
    }

    public boolean isSent() {
        return sentAt != null;
    }

    public MessageId getId() {
        return id;
    }

    public FlightId getFlightId() {
        return flightId;
    }

    public MessageType getType() {
        return type;
    }

    public FormatVersion getFormatVersion() {
        return formatVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getBody() {
        return body;
    }

    public @Nullable Instant getSentAt() {
        return sentAt;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        return this == other || other instanceof OutgoingMessage message && id.equals(message.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
