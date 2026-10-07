package be.kdg.airfreight.messaging.infrastructure.messaging;

import java.nio.charset.StandardCharsets;

import be.kdg.airfreight.messaging.application.FlightEventService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** All events of a flight arrive on one queue and are read by one consumer, so they stay in order. */
@Component
class FlightEventListener {
    private final FlightEventService flightEvents;

    FlightEventListener(FlightEventService flightEvents) {
        this.flightEvents = flightEvents;
    }

    @RabbitListener(queues = MessagingConfig.FLIGHTS_QUEUE)
    void on(Message message) {
        flightEvents.receiveFlightEvent(new String(message.getBody(), StandardCharsets.UTF_8));
    }
}
