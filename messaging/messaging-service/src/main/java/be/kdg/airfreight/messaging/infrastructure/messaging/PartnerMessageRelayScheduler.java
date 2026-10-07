package be.kdg.airfreight.messaging.infrastructure.messaging;

import be.kdg.airfreight.messaging.application.PartnerMessageRelay;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Empties the partner outbox on a timer. After an outage the next run sends what was waiting. */
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
