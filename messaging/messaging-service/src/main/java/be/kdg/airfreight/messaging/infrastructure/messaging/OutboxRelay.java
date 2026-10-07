package be.kdg.airfreight.messaging.infrastructure.messaging;

import java.nio.charset.StandardCharsets;
import java.time.Clock;

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
 */
@Slf4j
@Component
public class OutboxRelay {
    private final OutboxEventRepository outbox;
    private final RabbitTemplate rabbit;
    private final Clock clock;

    OutboxRelay(OutboxEventRepository outbox, RabbitTemplate rabbit, Clock clock) {
        this.outbox = outbox;
        this.rabbit = rabbit;
        this.clock = clock;
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
                rabbit.send(MessagingConfig.MESSAGING_EVENTS, event.getType(), message);
            } catch (RuntimeException e) {
                log.warn("Event {} ({}) could not be published and stays in the outbox: {}",
                    event.getMessageId(), event.getType(), e.getMessage());
                return;
            }
            event.markSent(clock.instant());
            log.info("Event {} ({}) published", event.getMessageId(), event.getType());
        }
    }
}
