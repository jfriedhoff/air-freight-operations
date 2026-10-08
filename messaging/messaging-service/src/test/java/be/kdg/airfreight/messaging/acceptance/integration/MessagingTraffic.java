package be.kdg.airfreight.messaging.acceptance.integration;

import be.kdg.airfreight.messaging.domain.outgoingmessage.OutgoingMessage;
import be.kdg.airfreight.messaging.domain.receivedmessage.ReceivedMessage;
import be.kdg.airfreight.messaging.infrastructure.messaging.OutboxEventRepository;
import be.kdg.airfreight.messaging.infrastructure.persistence.jpa.JpaOutgoingMessage;
import be.kdg.airfreight.messaging.infrastructure.persistence.jpa.JpaOutgoingMessageRepository;
import be.kdg.airfreight.messaging.infrastructure.persistence.jpa.JpaReceivedMessage;
import be.kdg.airfreight.messaging.infrastructure.persistence.jpa.JpaReceivedMessageRepository;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.awaitility.Awaitility.await;

/**
 * What goes into Messaging and what comes out. The senders (network planning, the cargo booking system,
 * Flight Operations, Load Planning) are played by publishing on their exchange, as they do, so the
 * listeners of Messaging pick the message up. What comes out is read from the database: the inbox
 * (received messages), the outbox for the other units and the partner messages.
 */
@Component
public class MessagingTraffic {
    /** A message with an error is not retried: it is set aside at once. */
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final String PARTNERS_QUEUE = "partners.all";

    private final RabbitTemplate rabbit;
    private final JsonMapper mapper;
    private final JpaReceivedMessageRepository received;
    private final JpaOutgoingMessageRepository outgoing;
    private final OutboxEventRepository outbox;

    public MessagingTraffic(RabbitTemplate rabbit, JsonMapper mapper, JpaReceivedMessageRepository received,
                            JpaOutgoingMessageRepository outgoing, OutboxEventRepository outbox) {
        this.rabbit = rabbit;
        this.mapper = mapper;
        this.received = received;
        this.outgoing = outgoing;
        this.outbox = outbox;
    }

    /** Publishes the message as JSON, as its sender does; its "type" is the routing key. */
    public void send(Stream stream, Map<String, Object> message) {
        send(stream, String.valueOf(message.get("type")), mapper.writeValueAsString(message));
    }

    /** Publishes any body, also one that is not JSON. */
    public void send(Stream stream, String routingKey, String body) {
        rabbit.send(stream.exchange, routingKey, MessageBuilder.withBody(body.getBytes(StandardCharsets.UTF_8))
            .setContentType("application/json")
            .build());
    }

    /** Waits until Messaging has processed or set aside the message with this id. */
    public void awaitReceived(UUID messageId) {
        await().atMost(TIMEOUT).until(() -> received.existsById(messageId));
    }

    /**
     * Returns once every message sent so far on this stream has been handled, also one that leaves no trace
     * (a duplicate, unreadable JSON). One consumer per queue handles messages in order, so a new message
     * sent after them is handled after them: a message with only an id, which Messaging sets aside.
     */
    public void waitUntilHandled(Stream stream) {
        UUID followUp = UUID.randomUUID();
        send(stream, Map.of("messageId", followUp.toString(), "type", stream.followUpType, "version", 1));
        awaitReceived(followUp);
    }

    public Optional<ReceivedMessage> receivedMessage(UUID messageId) {
        return received.findById(messageId).map(JpaReceivedMessage::toDomain);
    }

    public List<ReceivedMessage> receivedMessages() {
        return received.findAll().stream().map(JpaReceivedMessage::toDomain).toList();
    }

    /** The bodies of the events of this type that Messaging passes on to the other units. */
    public List<Map<String, Object>> passedOn(String type) {
        return outbox.findAll().stream()
            .filter(event -> event.getType().equals(type))
            .map(event -> readJson(event.getBody()))
            .toList();
    }

    public List<OutgoingMessage> partnerMessages() {
        return outgoing.findAll().stream().map(JpaOutgoingMessage::toDomain).toList();
    }

    /** Takes partner messages from the queue until the one with this id; others (older ones) are dropped. */
    public Message nextPartnerMessage(UUID messageId) {
        Message[] found = new Message[1];
        await().atMost(TIMEOUT).until(() -> {
            Message message = rabbit.receive(PARTNERS_QUEUE, 500);
            if (message != null && messageId.toString().equals(message.getMessageProperties().getMessageId())) {
                found[0] = message;
            }
            return found[0] != null;
        });
        return found[0];
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> readJson(String json) {
        return mapper.readValue(json, Map.class);
    }

    /** The exchange of each sender, and a type its queue accepts for the follow-up of waitUntilHandled. */
    public enum Stream {
        SCHEDULE("network-planning.messages", "network-planning.ssm"),
        BOOKINGS("cargo-booking.messages", "cargo-booking.uld"),
        FLIGHTS("flight-operations.events", "flight-operations.flight.announced"),
        LOADS("load-planning.events", "load-planning.flight-cargo.load-finalized");

        private final String exchange;
        private final String followUpType;

        Stream(String exchange, String followUpType) {
            this.exchange = exchange;
            this.followUpType = followUpType;
        }
    }
}
