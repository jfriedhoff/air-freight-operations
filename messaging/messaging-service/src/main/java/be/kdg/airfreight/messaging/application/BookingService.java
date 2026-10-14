package be.kdg.airfreight.messaging.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

import be.kdg.airfreight.messaging.domain.DomainRuleViolationException;
import be.kdg.airfreight.messaging.domain.MessageType;
import be.kdg.airfreight.messaging.domain.booking.UldBookingReceived;
import be.kdg.airfreight.messaging.domain.booking.UldMessage;
import be.kdg.airfreight.messaging.domain.receivedmessage.MessageReader;
import be.kdg.airfreight.messaging.domain.receivedmessage.ReceivedMessage;
import be.kdg.airfreight.messaging.domain.receivedmessage.ReceivedMessageRepository;
import be.kdg.airfreight.messaging.domain.telegram.DateDerivation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class BookingService {
    private final Inbox inbox;
    private final MessageReader<UldMessage> reader;
    private final DateDerivation dateDerivation;
    private final ReceivedMessageRepository received;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public BookingService(Inbox inbox, MessageReader<UldMessage> reader, DateDerivation dateDerivation,
                          ReceivedMessageRepository received, ApplicationEventPublisher events, Clock clock) {
        this.inbox = inbox;
        this.reader = reader;
        this.dateDerivation = dateDerivation;
        this.received = received;
        this.events = events;
        this.clock = clock;
    }

    /** A ULD message names its flight by the day of the month; the date is derived once and then fixed. */
    @Transactional
    public void receiveUldMessage(String body) {
        Instant now = clock.instant();
        inbox.read(MessageType.ULD, body, reader, now).ifPresent(message -> {
            LocalDate scheduledDepartureDate;
            try {
                scheduledDepartureDate = dateDerivation.deriveDay(message.dayOfMonth(), now);
            } catch (DomainRuleViolationException e) {
                inbox.reject(message.id(), MessageType.ULD, body, e.getMessage(), now);
                return;
            }
            UldBookingReceived booking = message.bookingOn(scheduledDepartureDate, now);
            received.save(ReceivedMessage.processed(message.id(), MessageType.ULD, message.version(), now, body,
                booking.flightId(), scheduledDepartureDate));
            events.publishEvent(booking);
            log.info("ULD message {} processed: {} booked on {} of {}", message.id().id(),
                message.uldCode().value(), message.flightNumber().value(), scheduledDepartureDate);
        });
    }
}
