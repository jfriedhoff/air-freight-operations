package be.kdg.airfreight.flightoperations.infrastructure.messaging;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Flight Operations declares its own exchange, the one queue it reads from with its bindings on
 * Messaging's exchange, and its dead letters. Their names come from AmqpProperties; the routing keys
 * are fixed. There is no JSON MessageConverter bean on purpose: Spring would run it on every incoming
 * message before the listener.
 */
@Configuration
@EnableConfigurationProperties(AmqpProperties.class)
class MessagingConfig {
    static final String FLIGHT_SCHEDULED = "messaging.flight.scheduled";
    static final String CPM_CREATED = "messaging.cpm.created";

    @Bean
    Declarables topology(AmqpProperties amqp) {
        TopicExchange flightOperationsEvents = new TopicExchange(amqp.eventsExchange());
        // Declared here as well as in Messaging: declaring is idempotent, and this service may start first.
        TopicExchange messagingEvents = new TopicExchange(amqp.messagingEventsExchange());
        FanoutExchange deadLetters = new FanoutExchange(amqp.deadLetterExchange());

        // One queue for everything this service follows, read by one consumer: the order is kept.
        Queue queue = QueueBuilder.durable(amqp.queue()).deadLetterExchange(amqp.deadLetterExchange())
            .singleActiveConsumer().build();
        Queue deadLetterQueue = QueueBuilder.durable(amqp.deadLetterQueue()).build();

        return new Declarables(flightOperationsEvents, messagingEvents, deadLetters, queue, deadLetterQueue,
            BindingBuilder.bind(queue).to(messagingEvents).with(FLIGHT_SCHEDULED),
            BindingBuilder.bind(queue).to(messagingEvents).with(CPM_CREATED),
            BindingBuilder.bind(deadLetterQueue).to(deadLetters));
    }

    /** Spring Boot gives this recoverer to the listener: it decides what happens after the last retry. */
    @Bean
    MessageRecoverer setAsideRecoverer(AmqpProperties amqp) {
        return new SetAsideRecoverer(amqp.deadLetterQueue());
    }
}
