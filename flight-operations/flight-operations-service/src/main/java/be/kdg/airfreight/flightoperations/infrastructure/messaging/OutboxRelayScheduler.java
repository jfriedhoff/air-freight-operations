package be.kdg.airfreight.flightoperations.infrastructure.messaging;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs the relay on a timer. The tests switch the timer off, so their events wait in the outbox until a scenario
 * runs the relay itself.
 * A run that fails before it can publish, such as on a database that cannot be reached, is logged as an error by
 * Spring's scheduler, with its stack trace; the next run tries again.
 */
@Component
@ConditionalOnProperty(name = "flight-operations.outbox.relay-enabled", havingValue = "true", matchIfMissing = true)
class OutboxRelayScheduler {
    private final OutboxRelay relay;

    OutboxRelayScheduler(OutboxRelay relay) {
        this.relay = relay;
    }

    @Scheduled(fixedDelayString = "${flight-operations.outbox.delay-ms}")
    void sendPending() {
        relay.sendPending();
    }
}
