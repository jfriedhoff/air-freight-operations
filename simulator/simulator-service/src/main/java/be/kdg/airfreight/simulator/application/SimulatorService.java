package be.kdg.airfreight.simulator.application;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
            UUID messageId = UUID.randomUUID();
            publisher.send(record, messageId, clock.instant());
            log.info("SSM {} sent for flight {}", messageId, record.flightNumber());
            messageIds.add(messageId);
        }
        return messageIds;
    }

    public List<UUID> sendBookings(List<BookingRecord> records) {
        List<UUID> messageIds = new ArrayList<>();
        for (BookingRecord record : records) {
            UUID messageId = UUID.randomUUID();
            publisher.send(record, messageId, clock.instant());
            log.info("ULD message {} sent for {} on flight {}", messageId, record.uldCode(), record.flightNumber());
            messageIds.add(messageId);
        }
        return messageIds;
    }
}
