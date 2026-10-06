package be.kdg.airfreight.messaging.infrastructure.messaging;

import java.util.UUID;

import be.kdg.airfreight.messaging.domain.booking.UldBookingReceived;
import be.kdg.airfreight.messaging.domain.flight.FlightEvent;
import be.kdg.airfreight.messaging.domain.load.CpmCreated;
import be.kdg.airfreight.messaging.domain.schedule.FlightScheduled;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.json.JsonMapper;

/**
 * Turns the events of this context into messages for the other contexts and publishes them on the
 * Messaging exchange, with the message type as routing key. After the commit, on the task executor:
 * a use case that rolls back publishes nothing. There is no outbox behind this: if the broker is
 * unreachable at that moment, the event is lost and only logged.
 */
@Slf4j
@Component
class AmqpEventPublisher {
    private final RabbitTemplate rabbit;
    private final JsonMapper mapper;

    AmqpEventPublisher(RabbitTemplate rabbit, JsonMapper mapper) {
        this.rabbit = rabbit;
        this.mapper = mapper;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    void on(FlightScheduled event) {
        UUID messageId = UUID.randomUUID();
        publish(MessageTypes.FLIGHT_SCHEDULED, messageId, new FlightScheduledJson(messageId,
            MessageTypes.FLIGHT_SCHEDULED, MessageTypes.VERSION, event.occurredOn(),
            event.flightNumber().value(), event.departureAirport().value(), event.arrivalAirport().value(),
            event.registration().value(), event.std(), event.sta()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    void on(UldBookingReceived event) {
        UUID messageId = UUID.randomUUID();
        publish(MessageTypes.ULD_BOOKING_RECEIVED, messageId, new UldBookingReceivedJson(messageId,
            MessageTypes.ULD_BOOKING_RECEIVED, MessageTypes.VERSION, event.occurredOn(),
            event.flightId().flightNumber().value(), event.flightId().scheduledDepartureDate(),
            event.flightId().departureAirport().value(), event.uldCode(), event.weight(), event.weightUnit(),
            event.destination().value()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    void on(CpmCreated event) {
        UUID messageId = UUID.randomUUID();
        publish(MessageTypes.CPM_CREATED, messageId, new CpmCreatedJson(messageId, MessageTypes.CPM_CREATED,
            MessageTypes.VERSION, event.occurredOn(), event.flightId().flightNumber().value(),
            event.flightId().scheduledDepartureDate(), event.flightId().departureAirport().value()));
    }

    /**
     * A flight event from Flight Operations is passed on as it is, under Messaging's own type. One executor
     * thread publishes them in the order they were committed, so their order survives the passing on.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    void on(FlightEvent event) {
        String type = MessageTypes.forwardedTypeOf(event);
        UUID messageId = UUID.randomUUID();
        publish(type, messageId, FlightEventJson.from(event, messageId, type));
    }

    /** Written as JSON here, with Boot's JsonMapper: dates travel as ISO text. */
    private void publish(String type, UUID messageId, Object json) {
        try {
            Message message = MessageBuilder.withBody(mapper.writeValueAsBytes(json))
                .setContentType("application/json")
                .setMessageId(messageId.toString())
                .build();
            rabbit.send(MessagingConfig.MESSAGING_EVENTS, type, message);
            log.debug("Published {}", type);
        } catch (RuntimeException e) {
            log.error("Event {} could not be published and is lost: {}", type, e.getMessage());
        }
    }
}
