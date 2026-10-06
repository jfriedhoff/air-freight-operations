package be.kdg.airfreight.messaging.domain.receivedmessage;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

import be.kdg.airfreight.messaging.domain.FlightId;
import be.kdg.airfreight.messaging.domain.FormatVersion;
import be.kdg.airfreight.messaging.domain.MessageId;
import be.kdg.airfreight.messaging.domain.MessageType;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.jspecify.annotations.Nullable;

/** The record of one incoming message: processed once, or set aside with the reason. Never changes. */
@AggregateRoot
public class ReceivedMessage {
    @Identity
    private final MessageId id;
    private final MessageType type;
    private final @Nullable FormatVersion formatVersion;
    private final Instant receivedAt;
    private final String body;
    private final ReceivedMessageStatus status;
    private final @Nullable String rejectionReason;
    private final @Nullable FlightId flightId;
    private final @Nullable LocalDate derivedFlightDate;

    private ReceivedMessage(MessageId id, MessageType type, @Nullable FormatVersion formatVersion,
                            Instant receivedAt, String body, ReceivedMessageStatus status,
                            @Nullable String rejectionReason, @Nullable FlightId flightId,
                            @Nullable LocalDate derivedFlightDate) {
        this.id = Objects.requireNonNull(id, "Id cannot be null");
        this.type = Objects.requireNonNull(type, "Type cannot be null");
        this.formatVersion = formatVersion;
        this.receivedAt = Objects.requireNonNull(receivedAt, "Reception time cannot be null");
        this.body = Objects.requireNonNull(body, "Body cannot be null");
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.rejectionReason = rejectionReason;
        this.flightId = flightId;
        this.derivedFlightDate = derivedFlightDate;
    }

    public static ReceivedMessage processed(MessageId id, MessageType type, FormatVersion formatVersion,
                                            Instant receivedAt, String body, @Nullable FlightId flightId,
                                            @Nullable LocalDate derivedFlightDate) {
        return new ReceivedMessage(id, type, Objects.requireNonNull(formatVersion, "Format version cannot be null"),
            receivedAt, body, ReceivedMessageStatus.PROCESSED, null, flightId, derivedFlightDate);
    }

    public static ReceivedMessage rejected(MessageId id, MessageType type, Instant receivedAt, String body,
                                           String reason) {
        return new ReceivedMessage(id, type, null, receivedAt, body, ReceivedMessageStatus.REJECTED,
            Objects.requireNonNull(reason, "Reason cannot be null"), null, null);
    }

    public static ReceivedMessage hydrate(MessageId id, MessageType type, @Nullable FormatVersion formatVersion,
                                          Instant receivedAt, String body, ReceivedMessageStatus status,
                                          @Nullable String rejectionReason, @Nullable FlightId flightId,
                                          @Nullable LocalDate derivedFlightDate) {
        return new ReceivedMessage(id, type, formatVersion, receivedAt, body, status, rejectionReason, flightId,
            derivedFlightDate);
    }

    public MessageId getId() {
        return id;
    }

    public MessageType getType() {
        return type;
    }

    public @Nullable FormatVersion getFormatVersion() {
        return formatVersion;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public String getBody() {
        return body;
    }

    public ReceivedMessageStatus getStatus() {
        return status;
    }

    public @Nullable String getRejectionReason() {
        return rejectionReason;
    }

    public @Nullable FlightId getFlightId() {
        return flightId;
    }

    public @Nullable LocalDate getDerivedFlightDate() {
        return derivedFlightDate;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        return this == other || other instanceof ReceivedMessage message && id.equals(message.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
