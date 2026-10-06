package be.kdg.airfreight.messaging.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import be.kdg.airfreight.messaging.domain.MessageType;
import be.kdg.airfreight.messaging.domain.receivedmessage.MessageReader;
import be.kdg.airfreight.messaging.domain.receivedmessage.ReceivedMessage;
import be.kdg.airfreight.messaging.domain.receivedmessage.ReceivedMessageRepository;
import be.kdg.airfreight.messaging.domain.schedule.FlightScheduled;
import be.kdg.airfreight.messaging.domain.schedule.Ssm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class ScheduleService {
    private final Inbox inbox;
    private final MessageReader<Ssm> reader;
    private final ReceivedMessageRepository received;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public ScheduleService(Inbox inbox, MessageReader<Ssm> reader, ReceivedMessageRepository received,
                           ApplicationEventPublisher events, Clock clock) {
        this.inbox = inbox;
        this.reader = reader;
        this.received = received;
        this.events = events;
        this.clock = clock;
    }

    /** An SSM announces one flight per matching weekday in its period. */
    @Transactional
    public void receiveSsm(String body) {
        Instant now = clock.instant();
        inbox.read(MessageType.SSM, body, reader, now).ifPresent(ssm -> {
            received.save(ReceivedMessage.processed(ssm.id(), MessageType.SSM, ssm.version(), now, body, null, null));
            List<FlightScheduled> flights = ssm.series().flightsScheduled(now);
            flights.forEach(events::publishEvent);
            log.info("SSM {} processed: {} flights scheduled for {}", ssm.id().id(), flights.size(),
                ssm.series().flightNumber().value());
        });
    }
}
