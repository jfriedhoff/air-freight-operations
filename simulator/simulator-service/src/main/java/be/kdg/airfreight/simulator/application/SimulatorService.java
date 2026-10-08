package be.kdg.airfreight.simulator.application;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import be.kdg.airfreight.simulator.infrastructure.messaging.SsmMessage;
import be.kdg.airfreight.simulator.infrastructure.messaging.StreamPublisher;
import be.kdg.airfreight.simulator.infrastructure.messaging.UldMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Imitates the two source systems: one SSM per flight schedule record, one ULD message per booking
 * record. Not transactional: the simulator stores nothing.
 */
@Slf4j
@Service
public class SimulatorService {
    private final StreamPublisher publisher;
    private final Clock clock;

    public SimulatorService(StreamPublisher publisher, Clock clock) {
        this.publisher = publisher;
        this.clock = clock;
    }

    public List<UUID> sendSchedules(List<ScheduleRecord> records) {
        List<UUID> messageIds = new ArrayList<>();
        for (ScheduleRecord record : records) {
            SsmMessage ssm = record.toSsm(UUID.randomUUID(), clock.instant());
            publisher.send(ssm);
            log.info("SSM {} sent for flight {}", ssm.messageId(), ssm.flightNumber());
            messageIds.add(ssm.messageId());
        }
        return messageIds;
    }

    public List<UUID> sendBookings(List<BookingRecord> records) {
        List<UUID> messageIds = new ArrayList<>();
        for (BookingRecord record : records) {
            UldMessage uldMessage = record.toUldMessage(UUID.randomUUID(), clock.instant());
            publisher.send(uldMessage);
            log.info("ULD message {} sent for {} on flight {}", uldMessage.messageId(), uldMessage.uldCode(),
                uldMessage.flightNumber());
            messageIds.add(uldMessage.messageId());
        }
        return messageIds;
    }
}
