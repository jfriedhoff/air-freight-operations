package be.kdg.airfreight.simulator.infrastructure.messaging;

import java.time.Instant;
import java.util.UUID;

import be.kdg.airfreight.simulator.application.BookingRecord;
import be.kdg.airfreight.simulator.application.ScheduleRecord;
import be.kdg.airfreight.simulator.application.StreamPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * The only class in the simulator that knows there is a broker. Turns a record into the message of its source
 * system and publishes it as JSON on the exchange of its stream, with the message type as routing key and the
 * message id as AMQP message-id.
 */
@Slf4j
@Component
class AmqpStreamPublisher implements StreamPublisher {
    private final RabbitTemplate rabbit;
    private final JsonMapper mapper;
    private final AmqpProperties amqp;

    AmqpStreamPublisher(RabbitTemplate rabbit, JsonMapper mapper, AmqpProperties amqp) {
        this.rabbit = rabbit;
        this.mapper = mapper;
        this.amqp = amqp;
    }

    @Override
    public void send(ScheduleRecord schedule, UUID messageId, Instant createdAt) {
        SsmMessage ssm = SsmMessage.of(schedule, messageId, createdAt);
        send(amqp.networkPlanningMessagesExchange(), ssm.type(), ssm.messageId(), ssm);
    }

    @Override
    public void send(BookingRecord booking, UUID messageId, Instant createdAt) {
        UldMessage uldMessage = UldMessage.of(booking, messageId, createdAt);
        send(amqp.cargoBookingMessagesExchange(), uldMessage.type(), uldMessage.messageId(), uldMessage);
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
