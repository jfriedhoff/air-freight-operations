package be.kdg.airfreight.messaging.infrastructure.messaging;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The sending half of the event outbox. Publishes the events that are not sent yet on the Messaging
 * exchange, in the order they were stored, and stops at the first one RabbitMQ does not take, so
 * nothing overtakes. After an outage the next run sends what was waiting. A crash between sending
 * and marking sends an event again, with the same message id.
 * A run that fails before it can publish, such as on a database that cannot be reached, is logged as an error by
 * Spring's scheduler, with its stack trace; the next run tries again.
 */
@Slf4j
@Component
public class OutboxRelay {
    private static final Duration CONFIRM_TIMEOUT = Duration.ofSeconds(5);

    private final OutboxEventRepository outbox;
    private final RabbitTemplate rabbit;
    private final Clock clock;
    private final AmqpProperties amqp;

    OutboxRelay(OutboxEventRepository outbox, RabbitTemplate rabbit, Clock clock, AmqpProperties amqp) {
        this.outbox = outbox;
        this.rabbit = rabbit;
        this.clock = clock;
        this.amqp = amqp;
    }

    @Scheduled(fixedDelayString = "${messaging.outbox.delay-ms}")
    @Transactional
    public void sendPending() {
        for (OutboxEvent event : outbox.findBySentAtIsNullOrderBySequenceAsc()) {
            Message message = MessageBuilder.withBody(event.getBody().getBytes(StandardCharsets.UTF_8))
                .setContentType("application/json")
                .setMessageId(event.getMessageId().toString())
                .build();
            try {
                publish(event.getType(), message);
            } catch (RuntimeException e) {
                log.error("Event {} ({}) could not be published and stays in the outbox",
                    event.getMessageId(), event.getType(), e);
                return;
            }
            event.markSent(clock.instant());
            log.info("Event {} ({}) published", event.getMessageId(), event.getType());
        }
    }

    /**
     * Waits until RabbitMQ confirms it took the message. Without the confirm a message to an exchange that does not
     * exist would count as sent and be lost; now it stays in the outbox and the next run tries again. An exchange
     * that exists but has no queue bound yet still confirms: whoever follows the events has not subscribed yet.
     */
    private void publish(String routingKey, Message message) {
        rabbit.invoke(operations -> {
            operations.send(amqp.eventsExchange(), routingKey, message);
            operations.waitForConfirmsOrDie(CONFIRM_TIMEOUT.toMillis());
            return null;
        });
    }
}
