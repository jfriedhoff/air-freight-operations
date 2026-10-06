package be.kdg.airfreight.messaging.domain;

import java.util.Objects;
import java.util.UUID;

import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
public record MessageId(UUID id) {
    public MessageId {
        Objects.requireNonNull(id, "Id cannot be null");
    }

    public static MessageId create() {
        return new MessageId(UUID.randomUUID());
    }
}
