package be.kdg.airfreight.messaging.infrastructure.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The names of the exchanges and queues Messaging uses: the exchanges of the senders it follows, its own
 * exchanges and its queues. The tests give every one of them a name of their own, so a Messaging, Flight
 * Operations or Load Planning that runs at the same time never sees a test message.
 */
@ConfigurationProperties("messaging.amqp")
public record AmqpProperties(String networkPlanningMessagesExchange, String cargoBookingMessagesExchange,
                             String flightOperationsEventsExchange, String loadPlanningEventsExchange,
                             String eventsExchange, String partnerMessagesExchange,
                             String scheduleQueue, String bookingsQueue, String flightsQueue, String loadsQueue,
                             String partnersQueue, String deadLetterExchange, String deadLetterQueue) {
}
