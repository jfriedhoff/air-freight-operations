package be.kdg.airfreight.simulator.infrastructure.messaging;

import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The simulator declares the two exchanges it publishes on and nothing else: queues belong to the
 * consumer. Messaging declares the same exchanges; declaring is idempotent, so either may start first.
 */
@Configuration
@EnableConfigurationProperties(StreamProperties.class)
class MessagingConfig {
    @Bean
    Declarables streams(StreamProperties streams) {
        return new Declarables(new TopicExchange(streams.scheduleExchange()),
            new TopicExchange(streams.bookingsExchange()));
    }
}
