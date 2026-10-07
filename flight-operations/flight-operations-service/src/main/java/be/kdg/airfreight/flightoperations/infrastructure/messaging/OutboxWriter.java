package be.kdg.airfreight.flightoperations.infrastructure.messaging;

import java.time.Clock;
import java.util.UUID;

import be.kdg.airfreight.flightoperations.domain.flight.FlightEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Stores every flight event that other contexts must hear, as its JSON contract, in the transaction
 * of the use case that raised it: the flight and its event are saved together or not at all.
 * This is an @EventListener and not a @TransactionalEventListener on purpose: the latter runs after
 * the commit, when it is too late to be part of it.
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
    void on(FlightEvent event) {
        UUID messageId = UUID.randomUUID();
        FlightEventJson.from(event, messageId).ifPresent(message ->
            outbox.save(new OutboxEvent(messageId, message.type(), mapper.writeValueAsString(message),
                clock.instant())));
    }
}
