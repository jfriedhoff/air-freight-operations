package be.kdg.airfreight.messaging.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import be.kdg.airfreight.messaging.domain.MessageId;
import be.kdg.airfreight.messaging.domain.MessageType;
import be.kdg.airfreight.messaging.domain.load.LoadFinalized;
import be.kdg.airfreight.messaging.domain.outgoingmessage.MessageWriter;
import be.kdg.airfreight.messaging.domain.outgoingmessage.OutgoingMessageRepository;
import be.kdg.airfreight.messaging.domain.receivedmessage.MessageReader;
import be.kdg.airfreight.messaging.domain.receivedmessage.ReceivedMessage;
import be.kdg.airfreight.messaging.domain.receivedmessage.ReceivedMessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class LoadService {
    private final Inbox inbox;
    private final MessageReader<LoadFinalized> reader;
    private final List<MessageWriter<LoadFinalized>> writers;
    private final ReceivedMessageRepository received;
    private final OutgoingMessageRepository outgoing;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public LoadService(Inbox inbox, MessageReader<LoadFinalized> reader,
                       List<MessageWriter<LoadFinalized>> writers, ReceivedMessageRepository received,
                       OutgoingMessageRepository outgoing, ApplicationEventPublisher events, Clock clock) {
        this.inbox = inbox;
        this.reader = reader;
        this.writers = writers;
        this.received = received;
        this.outgoing = outgoing;
        this.events = events;
        this.clock = clock;
    }

    /** A finalized load becomes a CPM for the partners, and Flight Operations is told the CPM exists. */
    @Transactional
    public void receiveLoadFinalized(String body) {
        Instant now = clock.instant();
        inbox.read(MessageType.LOAD_FINALIZED, body, reader, now).ifPresent(load -> {
            received.save(ReceivedMessage.processed(load.id(), MessageType.LOAD_FINALIZED, load.version(), now,
                body, load.flightId(), null));
            writers.stream()
                .filter(writer -> writer.writes(load))
                .map(writer -> writer.write(load, MessageId.create(), now))
                .forEach(outgoing::save);
            events.publishEvent(load.cpmCreated(now));
            log.info("Load finalized {} processed for {}: CPM written with {} ULDs", load.id().id(),
                load.flightId().flightNumber().value(), load.load().size());
        });
    }
}
