package be.kdg.airfreight.simulator.application;

import java.time.Instant;
import java.util.UUID;

/**
 * Sends a record on the stream it belongs to, as the message of the source system. The application only knows
 * this, not the message format or the broker.
 */
public interface StreamPublisher {
    void send(ScheduleRecord schedule, UUID messageId, Instant createdAt);

    void send(BookingRecord booking, UUID messageId, Instant createdAt);
}
