package be.kdg.airfreight.loadplanning.infrastructure.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The names of the exchanges and queues Load Planning uses. The tests give every one of them a name of their
 * own, so a Load Planning or Messaging that runs at the same time never sees a test message.
 */
@ConfigurationProperties("load-planning.amqp")
public record AmqpProperties(String eventsExchange, String messagingEventsExchange, String queue,
                             String deadLetterExchange, String deadLetterQueue) {
}
