package be.kdg.airfreight.messaging.application;

import java.time.Clock;

import be.kdg.airfreight.messaging.domain.outgoingmessage.OutgoingMessage;
import be.kdg.airfreight.messaging.domain.outgoingmessage.OutgoingMessageRepository;
import be.kdg.airfreight.messaging.domain.outgoingmessage.PartnerGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The sending half of the outbox: hands every unsent partner message over, oldest first. */
@Slf4j
@Service
public class PartnerMessageRelay {
    private final OutgoingMessageRepository outgoing;
    private final PartnerGateway partners;
    private final Clock clock;

    public PartnerMessageRelay(OutgoingMessageRepository outgoing, PartnerGateway partners, Clock clock) {
        this.outgoing = outgoing;
        this.partners = partners;
        this.clock = clock;
    }

    /** Stops at the first message that cannot be handed over, so later messages never overtake it. */
    @Transactional
    public void sendPending() {
        for (OutgoingMessage message : outgoing.findUnsentOldestFirst()) {
            try {
                partners.send(message);
            } catch (RuntimeException e) {
                log.warn("Partner message {} ({}) could not be sent and stays in the outbox: {}",
                    message.getId().id(), message.getType(), e.getMessage());
                return;
            }
            message.markSent(clock.instant());
            outgoing.save(message);
            log.info("Partner message {} ({}) sent", message.getId().id(), message.getType());
        }
    }
}
