package be.kdg.airfreight.messaging.domain.outgoingmessage;

import java.time.Instant;

import be.kdg.airfreight.messaging.domain.MessageId;

/** Writes one type of partner message. One implementation per message type. */
public interface MessageWriter<E> {
    boolean writes(E event);

    OutgoingMessage write(E event, MessageId id, Instant createdAt);
}
