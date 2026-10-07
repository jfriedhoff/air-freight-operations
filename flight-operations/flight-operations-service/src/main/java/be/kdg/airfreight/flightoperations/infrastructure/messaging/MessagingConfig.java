package be.kdg.airfreight.flightoperations.infrastructure.messaging;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Flight Operations declares its own exchange, the one queue it reads from with its bindings on
 * Messaging's exchange, and its dead letters. There is no JSON MessageConverter bean on purpose:
 * Spring would run it on every incoming message before the listener.
 */
@Configuration
class MessagingConfig {
    static final String FLIGHT_OPERATIONS_EVENTS = "flight-operations.events";
    static final String MESSAGING_EVENTS = "messaging.events";

    static final String QUEUE = "flight-operations.messaging-events";
    static final String DEAD_LETTER_EXCHANGE = "flight-operations.dlx";
    static final String DEAD_LETTER_QUEUE = "flight-operations.dlq";

    static final String FLIGHT_SCHEDULED = "messaging.flight.scheduled";
    static final String CPM_CREATED = "messaging.cpm.created";

    @Bean
    Declarables topology() {
        TopicExchange flightOperationsEvents = new TopicExchange(FLIGHT_OPERATIONS_EVENTS);
        // Declared here as well as in Messaging: declaring is idempotent, and this service may start first.
        TopicExchange messagingEvents = new TopicExchange(MESSAGING_EVENTS);
        FanoutExchange deadLetters = new FanoutExchange(DEAD_LETTER_EXCHANGE);

        // One queue for everything this service follows, read by one consumer: the order is kept.
        Queue queue = QueueBuilder.durable(QUEUE).deadLetterExchange(DEAD_LETTER_EXCHANGE)
            .singleActiveConsumer().build();
        Queue deadLetterQueue = QueueBuilder.durable(DEAD_LETTER_QUEUE).build();

        return new Declarables(flightOperationsEvents, messagingEvents, deadLetters, queue, deadLetterQueue,
            BindingBuilder.bind(queue).to(messagingEvents).with(FLIGHT_SCHEDULED),
            BindingBuilder.bind(queue).to(messagingEvents).with(CPM_CREATED),
            BindingBuilder.bind(deadLetterQueue).to(deadLetters));
    }
}
