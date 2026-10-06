package be.kdg.airfreight.messaging.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import be.kdg.airfreight.messaging.domain.DomainRuleViolationException;
import be.kdg.airfreight.messaging.domain.MessageId;
import be.kdg.airfreight.messaging.domain.MessageType;
import be.kdg.airfreight.messaging.domain.flight.FlightEvent;
import be.kdg.airfreight.messaging.domain.flight.FlightEventKind;
import be.kdg.airfreight.messaging.domain.outgoingmessage.MessageWriter;
import be.kdg.airfreight.messaging.domain.outgoingmessage.OutgoingMessage;
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
public class FlightEventService {
    private final Inbox inbox;
    private final MessageReader<FlightEvent> reader;
    private final List<MessageWriter<FlightEvent>> writers;
    private final ReceivedMessageRepository received;
    private final OutgoingMessageRepository outgoing;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public FlightEventService(Inbox inbox, MessageReader<FlightEvent> reader,
                              List<MessageWriter<FlightEvent>> writers, ReceivedMessageRepository received,
                              OutgoingMessageRepository outgoing, ApplicationEventPublisher events, Clock clock) {
        this.inbox = inbox;
        this.reader = reader;
        this.writers = writers;
        this.received = received;
        this.outgoing = outgoing;
        this.events = events;
        this.clock = clock;
    }

    /** A flight event becomes a partner message when a writer exists for it, and is passed on to Load Planning. */
    @Transactional
    public void receiveFlightEvent(String body) {
        Instant now = clock.instant();
        inbox.read(MessageType.FLIGHT_EVENT, body, reader, now).ifPresent(event -> {
            List<OutgoingMessage> partnerMessages;
            try {
                partnerMessages = writers.stream()
                    .filter(writer -> writer.writes(event))
                    .map(writer -> writer.write(event, MessageId.create(), now))
                    .toList();
            } catch (DomainRuleViolationException e) {
                inbox.reject(event.id(), MessageType.FLIGHT_EVENT, body, e.getMessage(), now);
                return;
            }
            if (event.kind() == FlightEventKind.REMOVED) {
                outgoing.deleteByFlight(event.flightId());
                received.deleteByFlight(event.flightId());
            }
            received.save(ReceivedMessage.processed(event.id(), MessageType.FLIGHT_EVENT, event.version(), now,
                body, event.flightId(), null));
            partnerMessages.forEach(outgoing::save);
            events.publishEvent(event);
            log.info("Flight event {} ({}) processed for {}: {} partner messages written", event.id().id(),
                event.kind(), event.flightId().flightNumber().value(), partnerMessages.size());
        });
    }
}
