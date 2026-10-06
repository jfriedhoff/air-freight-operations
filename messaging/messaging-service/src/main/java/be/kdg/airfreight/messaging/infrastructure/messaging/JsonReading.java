package be.kdg.airfreight.messaging.infrastructure.messaging;

import java.time.DateTimeException;
import java.util.UUID;
import java.util.function.Function;

import be.kdg.airfreight.messaging.domain.DomainRuleViolationException;
import be.kdg.airfreight.messaging.domain.InvalidMessageException;
import be.kdg.airfreight.messaging.domain.MessageId;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/** Reads a JSON message so that every problem ends as an InvalidMessageException with a one-line reason. */
final class JsonReading {
    private JsonReading() {
    }

    /**
     * First the messageId alone, so a message with a bad field can still be set aside under its own id;
     * then the whole message, translated to our own model.
     */
    static <J, T> T read(JsonMapper mapper, String body, Class<J> jsonType, Function<J, T> toDomain) {
        MessageId id = idOf(mapper, body);
        try {
            return toDomain.apply(mapper.readValue(body, jsonType));
        } catch (JacksonException | DomainRuleViolationException | NullPointerException | DateTimeException e) {
            throw new InvalidMessageException(id, reasonOf(e));
        }
    }

    static <V> V required(@Nullable V value, String field) {
        if (value == null) {
            throw new DomainRuleViolationException("Field '" + field + "' is missing");
        }
        return value;
    }

    private static MessageId idOf(JsonMapper mapper, String body) {
        Envelope envelope;
        try {
            envelope = mapper.readValue(body, Envelope.class);
        } catch (JacksonException e) {
            throw new InvalidMessageException(null, "The message is not readable JSON");
        }
        if (envelope == null || envelope.messageId() == null) {
            throw new InvalidMessageException(null, "The message has no messageId");
        }
        return new MessageId(envelope.messageId());
    }

    private static String reasonOf(RuntimeException e) {
        String message = e.getMessage();
        if (message == null) {
            return "A required field is missing";
        }
        return message.lines().findFirst().orElse(message);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Envelope(@Nullable UUID messageId) {
    }
}
