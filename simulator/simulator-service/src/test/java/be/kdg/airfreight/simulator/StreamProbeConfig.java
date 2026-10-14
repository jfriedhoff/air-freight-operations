package be.kdg.airfreight.simulator;

import be.kdg.airfreight.simulator.infrastructure.messaging.AmqpProperties;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The tests play the consumer: one queue per stream that receives everything published on it.
 * Durable, because RabbitMQ 4 refuses a non-durable queue that is not exclusive; StreamProbe empties them.
 */
@Configuration
class StreamProbeConfig {
    static final String SCHEDULE_QUEUE = "simulator-test.schedule";
    static final String BOOKINGS_QUEUE = "simulator-test.bookings";

    @Bean
    Declarables probeQueues(AmqpProperties amqp) {
        Queue schedule = QueueBuilder.durable(SCHEDULE_QUEUE).build();
        Queue bookings = QueueBuilder.durable(BOOKINGS_QUEUE).build();
        return new Declarables(schedule, bookings,
            BindingBuilder.bind(schedule).to(new TopicExchange(amqp.networkPlanningMessagesExchange())).with("#"),
            BindingBuilder.bind(bookings).to(new TopicExchange(amqp.cargoBookingMessagesExchange())).with("#"));
    }
}
