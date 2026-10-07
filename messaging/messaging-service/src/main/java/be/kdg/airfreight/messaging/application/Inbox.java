package be.kdg.airfreight.messaging.application;

import java.time.Instant;
import java.util.Optional;

import be.kdg.airfreight.messaging.domain.InvalidMessageException;
import be.kdg.airfreight.messaging.domain.MessageId;
import be.kdg.airfreight.messaging.domain.MessageType;
import be.kdg.airfreight.messaging.domain.receivedmessage.IncomingMessage;
import be.kdg.airfreight.messaging.domain.receivedmessage.MessageReader;
import be.kdg.airfreight.messaging.domain.receivedmessage.ReceivedMessage;
import be.kdg.airfreight.messaging.domain.receivedmessage.ReceivedMessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Decides whether an incoming message goes on: not when it was seen before, not when it is invalid. */
@Slf4j
@Component
class Inbox {
    private final ReceivedMessageRepository received;

    Inbox(ReceivedMessageRepository received) {
        this.received = received;
    }

    /** The message, or nothing when it is a duplicate or invalid. An invalid message is set aside here. */
    <T extends IncomingMessage> Optional<T> read(MessageType type, String body, MessageReader<T> reader,
                                                 Instant now) {
        try {
            T message = reader.read(body);
            if (received.existsById(message.id())) {
                log.info("Duplicate {} message {} ignored", type, message.id().id());
                return Optional.empty();
            }
            return Optional.of(message);
        } catch (InvalidMessageException e) {
            reject(e.messageId().orElseGet(MessageId::create), type, body, e.getMessage(), now);
            return Optional.empty();
        }
    }

    void reject(MessageId id, MessageType type, String body, String reason, Instant now) {
        if (received.existsById(id)) {
            log.info("Duplicate {} message {} ignored", type, id.id());
            return;
        }
        received.save(ReceivedMessage.rejected(id, type, now, body, reason));
        log.warn("{} message {} rejected and set aside: {}", type, id.id(), reason);
    }
}
