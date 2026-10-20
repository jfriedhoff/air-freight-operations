package be.kdg.airfreight.simulator.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SimulatorServiceTest {
    private static final Instant NOW = Instant.parse("2026-01-02T08:00:00Z");

    private final RecordingPublisher publisher = new RecordingPublisher();
    private final SimulatorService simulator = new SimulatorService(publisher, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void oneSsmIsSentPerScheduleRecordInTheOrderOfTheRecords() {
        simulator.sendSchedules(List.of(scheduleFor("HJ221"), scheduleFor("3V11")));

        assertThat(publisher.schedules).extracting(sent -> sent.record().flightNumber()).containsExactly("HJ221", "3V11");
    }

    @Test
    void oneUldMessageIsSentPerBookingRecordInTheOrderOfTheRecords() {
        simulator.sendBookings(List.of(bookingOf("AKE16689R7"), bookingOf("PMC12345KL"), bookingOf("AKE22222R7")));

        assertThat(publisher.bookings).extracting(sent -> sent.record().uldCode())
            .containsExactly("AKE16689R7", "PMC12345KL", "AKE22222R7");
    }

    @Test
    void everyMessageGetsItsOwnIdAndTheIdsAreReturned() {
        List<UUID> ids = simulator.sendSchedules(List.of(scheduleFor("HJ221"), scheduleFor("HJ221")));

        assertThat(ids).hasSize(2).doesNotHaveDuplicates();
        assertThat(publisher.schedules).extracting(Sent::messageId).containsExactlyElementsOf(ids);
    }

    @Test
    void aMessageIsStampedWithTheMomentItIsSent() {
        simulator.sendBookings(List.of(bookingOf("AKE16689R7")));

        assertThat(publisher.bookings).extracting(Sent::createdAt).containsExactly(NOW);
    }

    private static ScheduleRecord scheduleFor(String flightNumber) {
        return new ScheduleRecord(LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 18), Set.of(DayOfWeek.MONDAY),
            LocalTime.of(6, 30), LocalTime.of(9, 45), flightNumber, "BRU", "ATH", "SXBAA");
    }

    private static BookingRecord bookingOf(String uldCode) {
        return new BookingRecord("HJ221", LocalDate.of(2026, 1, 9), "BRU", uldCode, new BigDecimal("1000"), "KG",
            "ATH");
    }

    private record Sent<R>(R record, UUID messageId, Instant createdAt) {
    }

    /** Keeps what was sent, instead of a broker. */
    private static class RecordingPublisher implements StreamPublisher {
        private final List<Sent<ScheduleRecord>> schedules = new ArrayList<>();
        private final List<Sent<BookingRecord>> bookings = new ArrayList<>();

        @Override
        public void send(ScheduleRecord schedule, UUID messageId, Instant createdAt) {
            schedules.add(new Sent<>(schedule, messageId, createdAt));
        }

        @Override
        public void send(BookingRecord booking, UUID messageId, Instant createdAt) {
            bookings.add(new Sent<>(booking, messageId, createdAt));
        }
    }
}
