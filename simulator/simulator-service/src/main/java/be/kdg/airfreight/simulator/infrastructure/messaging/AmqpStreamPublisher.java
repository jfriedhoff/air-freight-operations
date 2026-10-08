package be.kdg.airfreight.simulator.infrastructure.messaging;

import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * The only class in the simulator that knows there is a broker. Publishes the message as JSON on the
 * exchange of its stream, with the message type as routing key and the message id as AMQP message-id.
 */
@Slf4j
@Component
class AmqpStreamPublisher implements StreamPublisher {
    private final RabbitTemplate rabbit;
    private final JsonMapper mapper;
    private final StreamProperties streams;

    AmqpStreamPublisher(RabbitTemplate rabbit, JsonMapper mapper, StreamProperties streams) {
        this.rabbit = rabbit;
        this.mapper = mapper;
        this.streams = streams;
    }

    @Override
    public void send(SsmMessage ssm) {
        send(streams.scheduleExchange(), ssm.type(), ssm.messageId(), ssm);
    }

    @Override
    public void send(UldMessage uldMessage) {
        send(streams.bookingsExchange(), uldMessage.type(), uldMessage.messageId(), uldMessage);
    }

    private void send(String exchange, String type, UUID messageId, Object body) {
        Message message = MessageBuilder.withBody(mapper.writeValueAsBytes(body))
            .setContentType("application/json")
            .setMessageId(messageId.toString())
            .build();
        rabbit.send(exchange, type, message);
        log.debug("Message {} ({}) published on {}", messageId, type, exchange);
    }
}
