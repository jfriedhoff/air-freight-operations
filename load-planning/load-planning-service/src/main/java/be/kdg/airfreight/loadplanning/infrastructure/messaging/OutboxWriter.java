package be.kdg.airfreight.loadplanning.infrastructure.messaging;

import java.time.Clock;
import java.util.UUID;

import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadFinalized;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Stores the one event of this context that other contexts must hear, as its JSON contract, in the
 * transaction of the use case that raised it: the finalized load and its event are saved together or
 * not at all. An @EventListener and not a @TransactionalEventListener on purpose: the latter runs
 * after the commit, when it is too late to be part of it.
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
    void on(LoadFinalized event) {
        UUID messageId = UUID.randomUUID();
        LoadFinalizedJson message = LoadFinalizedJson.from(event, messageId);
        outbox.save(new OutboxEvent(messageId, message.type(), mapper.writeValueAsString(message),
            clock.instant()));
    }
}
