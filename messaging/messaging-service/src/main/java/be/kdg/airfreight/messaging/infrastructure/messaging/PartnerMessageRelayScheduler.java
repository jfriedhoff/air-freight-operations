package be.kdg.airfreight.messaging.infrastructure.messaging;

import be.kdg.airfreight.messaging.application.PartnerMessageRelay;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Empties the partner outbox on a timer. After an outage the next run sends what was waiting. A run that fails
 * before it can send, such as on a database that cannot be reached, is logged as an error by Spring's scheduler,
 * with its stack trace; the next run tries again.
 */
@Component
class PartnerMessageRelayScheduler {
    private final PartnerMessageRelay relay;

    PartnerMessageRelayScheduler(PartnerMessageRelay relay) {
        this.relay = relay;
    }

    @Scheduled(fixedDelayString = "${messaging.relay.delay-ms}")
    void sendPending() {
        relay.sendPending();
    }
}
