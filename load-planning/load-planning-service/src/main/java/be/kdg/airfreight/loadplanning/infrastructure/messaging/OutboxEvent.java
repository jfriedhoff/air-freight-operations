package be.kdg.airfreight.loadplanning.infrastructure.messaging;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.jspecify.annotations.Nullable;

/** An event that still has to be published, or was. The sequence is the order in which they go out. */
@Entity
@Table(name = "outbox_events")
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private @Nullable Long sequence;

    @Column(nullable = false, unique = true)
    private UUID messageId;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(nullable = false)
    private Instant createdAt;

    private @Nullable Instant sentAt;

    protected OutboxEvent() {
    }

    OutboxEvent(UUID messageId, String type, String body, Instant createdAt) {
        this.messageId = messageId;
        this.type = type;
        this.body = body;
        this.createdAt = createdAt;
    }

    void markSent(Instant at) {
        this.sentAt = at;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public String getType() {
        return type;
    }

    public String getBody() {
        return body;
    }

    public @Nullable Instant getSentAt() {
        return sentAt;
    }
}
