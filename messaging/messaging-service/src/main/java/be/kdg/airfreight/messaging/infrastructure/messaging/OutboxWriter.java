package be.kdg.airfreight.messaging.infrastructure.messaging;

import java.time.Clock;
import java.util.UUID;

import be.kdg.airfreight.messaging.domain.booking.UldBookingReceived;
import be.kdg.airfreight.messaging.domain.flight.FlightEvent;
import be.kdg.airfreight.messaging.domain.load.CpmCreated;
import be.kdg.airfreight.messaging.domain.schedule.FlightScheduled;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Turns the events of this context into messages for the other contexts and stores them, in the
 * transaction of the use case that raised them: the inbox record, the partner messages and these
 * events are saved together or not at all. An @EventListener and not a @TransactionalEventListener
 * on purpose: the latter runs after the commit, when it is too late to be part of it.
 */
@Component
class OutboxWriter {
    private final OutboxEventRepository outbox;
    private final JsonMapper mapper;
    private final Clock clock;

    OutboxWriter(OutboxEventRepository outbox, JsonMapper mapper, Clock clock) {
        this.outbox = outbox;
        this.mapper = mapper;
        this.clock = clock;
    }

    @EventListener
    void on(FlightScheduled event) {
        UUID messageId = UUID.randomUUID();
        store(MessageTypes.FLIGHT_SCHEDULED, messageId, new FlightScheduledJson(messageId,
            MessageTypes.FLIGHT_SCHEDULED, MessageTypes.VERSION, event.occurredOn(),
            event.flightNumber().value(), event.departureAirport().value(), event.arrivalAirport().value(),
            event.registration().value(), event.std(), event.sta()));
    }

    @EventListener
    void on(UldBookingReceived event) {
        UUID messageId = UUID.randomUUID();
        store(MessageTypes.ULD_BOOKING_RECEIVED, messageId, new UldBookingReceivedJson(messageId,
            MessageTypes.ULD_BOOKING_RECEIVED, MessageTypes.VERSION, event.occurredOn(),
            event.flightId().flightNumber().value(), event.flightId().scheduledDepartureDate(),
            event.flightId().departureAirport().value(), event.uldCode(), event.weight(), event.weightUnit(),
            event.destination().value()));
    }

    @EventListener
    void on(CpmCreated event) {
        UUID messageId = UUID.randomUUID();
        store(MessageTypes.CPM_CREATED, messageId, new CpmCreatedJson(messageId, MessageTypes.CPM_CREATED,
            MessageTypes.VERSION, event.occurredOn(), event.flightId().flightNumber().value(),
            event.flightId().scheduledDepartureDate(), event.flightId().departureAirport().value()));
    }

    /** A flight event from Flight Operations is passed on as it is, under Messaging's own type. */
    @EventListener
    void on(FlightEvent event) {
        String type = MessageTypes.forwardedTypeOf(event);
        UUID messageId = UUID.randomUUID();
        store(type, messageId, FlightEventJson.from(event, messageId, type));
    }

    /** Written as JSON here, with Boot's JsonMapper: dates travel as ISO text. */
    private void store(String type, UUID messageId, Object json) {
        outbox.save(new OutboxEvent(messageId, type, mapper.writeValueAsString(json), clock.instant()));
    }
}
