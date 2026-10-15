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
 * properties: for an MVT, whose body is only the telegram, that is the one place they are.
 */
@Component
class AmqpPartnerGateway implements PartnerGateway {
    static final String FORMAT_VERSION_HEADER = "formatVersion";

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
            .setHeader(FORMAT_VERSION_HEADER, message.getFormatVersion().number())
            .build();
        rabbit.send(amqp.partnerMessagesExchange(), routingKeyOf(message.getType()), amqpMessage);
    }

    private static String contentTypeOf(MessageType type) {
        return switch (type) {
            case FLM, CPM -> "application/json";
            default -> "text/plain";
        };
    }

    private static String routingKeyOf(MessageType type) {
        return switch (type) {
            case FLM -> "partner.flm";
            case MVT_ED -> "partner.mvt.ed";
            case MVT_EA -> "partner.mvt.ea";
            case MVT_AD -> "partner.mvt.ad";
            case MVT_AA -> "partner.mvt.aa";
            case CPM -> "partner.cpm";
            default -> throw new IllegalArgumentException(type + " is not a partner message");
        };
    }
}
