package be.kdg.airfreight.flightoperations.infrastructure.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The names of the exchanges and queues Flight Operations uses. The tests give every one of them a name of
 * their own, so a Flight Operations or Messaging that runs at the same time never sees a test message.
 */
@ConfigurationProperties("flight-operations.amqp")
public record AmqpProperties(String eventsExchange, String messagingEventsExchange, String queue,
                             String deadLetterExchange, String deadLetterQueue) {
}
