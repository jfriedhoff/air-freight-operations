package be.kdg.airfreight.loadplanning.infrastructure.messaging;

import java.util.ArrayList;
import java.util.List;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Load Planning declares its own exchange, the one queue it reads from with its bindings on Messaging's
 * exchange, and its dead letters. Their names come from AmqpProperties; the routing keys are fixed.
 * There is no JSON MessageConverter bean on purpose: Spring would run it on every incoming message before
 * the listener.
 */
@Configuration
@EnableConfigurationProperties(AmqpProperties.class)
class MessagingConfig {
    static final String FLIGHT_ANNOUNCED = "messaging.flight.announced";
    static final String DEPARTURE_ESTIMATE_CHANGED = "messaging.flight.departure-estimate-changed";
    static final String ARRIVAL_ESTIMATE_CHANGED = "messaging.flight.arrival-estimate-changed";
    static final String FLIGHT_DEPARTED = "messaging.flight.departed";
    static final String FLIGHT_LANDED = "messaging.flight.landed";
    static final String FLIGHT_REMOVED = "messaging.flight.removed";
    static final String ULD_BOOKING_RECEIVED = "messaging.uld-booking.received";

    static final String LOAD_FINALIZED = "load-planning.flight-cargo.load-finalized";

    @Bean
    Declarables topology(AmqpProperties amqp) {
        TopicExchange loadPlanningEvents = new TopicExchange(amqp.eventsExchange());
        // Declared here as well as in Messaging: declaring is idempotent, and this service may start first.
        TopicExchange messagingEvents = new TopicExchange(amqp.messagingEventsExchange());
        FanoutExchange deadLetters = new FanoutExchange(amqp.deadLetterExchange());

        // One queue for the flight events and the bookings, read by one consumer: the order is kept.
        Queue queue = QueueBuilder.durable(amqp.queue()).deadLetterExchange(amqp.deadLetterExchange())
            .singleActiveConsumer().build();
        Queue deadLetterQueue = QueueBuilder.durable(amqp.deadLetterQueue()).build();

        List<Declarable> declarables = new ArrayList<>(List.of(loadPlanningEvents, messagingEvents, deadLetters,
            queue, deadLetterQueue, BindingBuilder.bind(deadLetterQueue).to(deadLetters)));
        for (String routingKey : List.of(FLIGHT_ANNOUNCED, DEPARTURE_ESTIMATE_CHANGED, ARRIVAL_ESTIMATE_CHANGED,
            FLIGHT_DEPARTED, FLIGHT_LANDED, FLIGHT_REMOVED, ULD_BOOKING_RECEIVED)) {
            declarables.add(BindingBuilder.bind(queue).to(messagingEvents).with(routingKey));
        }
        return new Declarables(declarables);
    }
}
