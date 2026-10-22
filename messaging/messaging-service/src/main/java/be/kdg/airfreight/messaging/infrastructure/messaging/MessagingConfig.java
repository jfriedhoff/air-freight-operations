package be.kdg.airfreight.messaging.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * One topic exchange per publisher, one queue per stream that Messaging follows. Messaging declares the
 * exchanges of the other publishers as well: declaring is idempotent, and it may start first. The names
 * come from AmqpProperties; the routing keys are fixed.
 * There is no JSON MessageConverter bean on purpose: Spring would run it on every incoming message
 * before the listener, and an unreadable body would then fail there instead of reaching our readers.
 */
@Configuration
@EnableConfigurationProperties(AmqpProperties.class)
class MessagingConfig {
    @Bean
    Declarables topology(AmqpProperties amqp) {
        TopicExchange networkPlanning = new TopicExchange(amqp.networkPlanningMessagesExchange());
        TopicExchange cargoBooking = new TopicExchange(amqp.cargoBookingMessagesExchange());
        TopicExchange flightOperations = new TopicExchange(amqp.flightOperationsEventsExchange());
        TopicExchange loadPlanning = new TopicExchange(amqp.loadPlanningEventsExchange());
        TopicExchange messagingEvents = new TopicExchange(amqp.eventsExchange());
        TopicExchange partnerMessages = new TopicExchange(amqp.partnerMessagesExchange());
        FanoutExchange deadLetters = new FanoutExchange(amqp.deadLetterExchange());

        Queue schedule = inbound(amqp.scheduleQueue(), amqp);
        Queue bookings = inbound(amqp.bookingsQueue(), amqp);
        // The order of a flight's events matters. Single active consumer: RabbitMQ lets only one consumer
        // read this queue at a time, also when a second instance of the service is started.
        Queue flights = QueueBuilder.durable(amqp.flightsQueue()).deadLetterExchange(amqp.deadLetterExchange())
            .singleActiveConsumer().build();
        Queue loads = inbound(amqp.loadsQueue(), amqp);
        // Nobody is connected yet: this queue only makes the partner messages visible.
        Queue partners = QueueBuilder.durable(amqp.partnersQueue()).build();
        Queue deadLetterQueue = QueueBuilder.durable(amqp.deadLetterQueue()).build();

        return new Declarables(
            networkPlanning, cargoBooking, flightOperations, loadPlanning, messagingEvents, partnerMessages,
            deadLetters,
            schedule, bookings, flights, loads, partners, deadLetterQueue,
            bind(schedule, networkPlanning, "network-planning.#"),
            bind(bookings, cargoBooking, "cargo-booking.#"),
            bind(flights, flightOperations, "flight-operations.flight.#"),
            bind(loads, loadPlanning, "load-planning.flight-cargo.#"),
            bind(partners, partnerMessages, "#"),
            BindingBuilder.bind(deadLetterQueue).to(deadLetters));
    }

    /** A queue Messaging reads from: a message that keeps failing is set aside on the dead letter queue. */
    private static Queue inbound(String name, AmqpProperties amqp) {
        return QueueBuilder.durable(name).deadLetterExchange(amqp.deadLetterExchange()).build();
    }

    private static Binding bind(Queue queue, TopicExchange exchange, String pattern) {
        return BindingBuilder.bind(queue).to(exchange).with(pattern);
    }
}
