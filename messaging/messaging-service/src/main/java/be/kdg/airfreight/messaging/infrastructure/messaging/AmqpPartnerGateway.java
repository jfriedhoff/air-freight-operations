package be.kdg.airfreight.messaging.infrastructure.messaging;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import be.kdg.airfreight.messaging.domain.MessageType;
import be.kdg.airfreight.messaging.domain.outgoingmessage.OutgoingMessage;
import be.kdg.airfreight.messaging.domain.outgoingmessage.PartnerGateway;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Sends a partner message as it was written. Its id, creation time and format version travel as AMQP
 * properties and headers: for an MVT, whose body is only the telegram, that is the one place they are.
 * Header names and routing keys follow the partner examples.
 */
@Component
class AmqpPartnerGateway implements PartnerGateway {
    static final String MESSAGE_ID_HEADER = "message-id";
    static final String PRODUCED_AT_HEADER = "produced-at";
    static final String VERSION_HEADER = "version";

    private final RabbitTemplate rabbit;
    private final AmqpProperties amqp;

    AmqpPartnerGateway(RabbitTemplate rabbit, AmqpProperties amqp) {
        this.rabbit = rabbit;
        this.amqp = amqp;
    }

    @Override
    public void send(OutgoingMessage message) {
        Message amqpMessage = MessageBuilder
            .withBody(message.getBody().getBytes(StandardCharsets.UTF_8))
            .setContentType(contentTypeOf(message.getType()))
            .setContentEncoding(StandardCharsets.UTF_8.name())
            .setMessageId(message.getId().id().toString())
            .setTimestamp(Date.from(message.getCreatedAt()))
            .setHeader(MESSAGE_ID_HEADER, message.getId().id().toString())
            .setHeader(PRODUCED_AT_HEADER, message.getCreatedAt().toString())
            .setHeader(VERSION_HEADER, message.getFormatVersion().number())
            .build();
        // Waits for RabbitMQ's confirm: a message it did not take must stay in the outbox, not count as sent
        ConfirmedSend.send(rabbit, amqp.partnerMessagesExchange(), routingKeyOf(message.getType()), amqpMessage);
    }

    private static String contentTypeOf(MessageType type) {
        return switch (type) {
            case FLM, CPM -> "application/json";
            default -> "text/plain";
        };
    }

    private static String routingKeyOf(MessageType type) {
        return switch (type) {
            case FLM -> "flm";
            case MVT_ED -> "mvt.ed";
            case MVT_EA -> "mvt.ea";
            case MVT_AD -> "mvt.ad";
            case MVT_AA -> "mvt.aa";
            case CPM -> "cpm";
            default -> throw new IllegalArgumentException(type + " is not a partner message");
        };
    }
}
