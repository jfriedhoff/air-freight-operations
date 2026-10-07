package be.kdg.airfreight.messaging.domain;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

/** An incoming message that cannot be read or that breaks a rule. It is set aside, never applied. */
public class InvalidMessageException extends RuntimeException {
    private final transient @Nullable MessageId messageId;

    public InvalidMessageException(@Nullable MessageId messageId, String reason) {
        super(reason);
        this.messageId = messageId;
    }

    /** The id of the message, when the message was readable enough to find it. */
    public Optional<MessageId> messageId() {
        return Optional.ofNullable(messageId);
    }
}
