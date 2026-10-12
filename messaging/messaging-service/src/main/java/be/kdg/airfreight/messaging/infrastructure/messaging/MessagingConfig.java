package be.kdg.airfreight.messaging.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * One topic exchange per publisher, one queue per stream that Messaging follows. Messaging declares the
 * exchanges of the other publishers as well: declaring is idempotent, and it may start first.
 * There is no JSON MessageConverter bean on purpose: Spring would run it on every incoming message
 * before the listener, and an unreadable body would then fail there instead of reaching our readers.
 */
@Configuration
class MessagingConfig {
    static final String NETWORK_PLANNING_MESSAGES = "network-planning.messages";
    static final String CARGO_BOOKING_MESSAGES = "cargo-booking.messages";
    static final String FLIGHT_OPERATIONS_EVENTS = "flight-operations.events";
    static final String LOAD_PLANNING_EVENTS = "load-planning.events";
    static final String PARTNER_MESSAGES = "messaging.partner-messages";

    static final String SCHEDULE_QUEUE = "messaging.schedule";
    static final String BOOKINGS_QUEUE = "messaging.bookings";
    static final String FLIGHTS_QUEUE = "messaging.flights";
    static final String LOADS_QUEUE = "messaging.loads";
    static final String PARTNERS_QUEUE = "partners.all";

    static final String DEAD_LETTER_EXCHANGE = "messaging.dlx";
    static final String DEAD_LETTER_QUEUE = "messaging.dlq";

    @Bean
    Declarables topology(@Value("${messaging.events-exchange}") String eventsExchange) {
        TopicExchange networkPlanning = new TopicExchange(NETWORK_PLANNING_MESSAGES);
        TopicExchange cargoBooking = new TopicExchange(CARGO_BOOKING_MESSAGES);
        TopicExchange flightOperations = new TopicExchange(FLIGHT_OPERATIONS_EVENTS);
        TopicExchange loadPlanning = new TopicExchange(LOAD_PLANNING_EVENTS);
        TopicExchange messagingEvents = new TopicExchange(eventsExchange);
        TopicExchange partnerMessages = new TopicExchange(PARTNER_MESSAGES);
        FanoutExchange deadLetters = new FanoutExchange(DEAD_LETTER_EXCHANGE);

        Queue schedule = inbound(SCHEDULE_QUEUE);
        Queue bookings = inbound(BOOKINGS_QUEUE);
        // The order of a flight's events matters. Single active consumer: RabbitMQ lets only one consumer
        // read this queue at a time, also when a second instance of the service is started.
        Queue flights = QueueBuilder.durable(FLIGHTS_QUEUE).deadLetterExchange(DEAD_LETTER_EXCHANGE)
            .singleActiveConsumer().build();
        Queue loads = inbound(LOADS_QUEUE);
        // Nobody is connected yet: this queue only makes the partner messages visible.
        Queue partners = QueueBuilder.durable(PARTNERS_QUEUE).build();
        Queue deadLetterQueue = QueueBuilder.durable(DEAD_LETTER_QUEUE).build();

        return new Declarables(
            networkPlanning, cargoBooking, flightOperations, loadPlanning, messagingEvents, partnerMessages,
            deadLetters,
            schedule, bookings, flights, loads, partners, deadLetterQueue,
            bind(schedule, networkPlanning, "network-planning.#"),
            bind(bookings, cargoBooking, "cargo-booking.#"),
            bind(flights, flightOperations, "flight-operations.flight.#"),
            bind(loads, loadPlanning, "load-planning.flight-cargo.#"),
            bind(partners, partnerMessages, "partner.#"),
            BindingBuilder.bind(deadLetterQueue).to(deadLetters));
    }

    /** A queue Messaging reads from: a message that keeps failing is set aside on the dead letter queue. */
    private static Queue inbound(String name) {
        return QueueBuilder.durable(name).deadLetterExchange(DEAD_LETTER_EXCHANGE).build();
    }

    private static Binding bind(Queue queue, TopicExchange exchange, String pattern) {
        return BindingBuilder.bind(queue).to(exchange).with(pattern);
    }
}
