package be.kdg.airfreight.simulator.infrastructure.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The exchange of each stream the simulator imitates. The tests give both a name of their own, so a
 * Messaging that runs at the same time never gets a test flight or booking.
 */
@ConfigurationProperties("simulator.amqp")
public record AmqpProperties(String networkPlanningMessagesExchange, String cargoBookingMessagesExchange) {
}
