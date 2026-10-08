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

import be.kdg.airfreight.simulator.infrastructure.messaging.SsmMessage;
import be.kdg.airfreight.simulator.infrastructure.messaging.StreamPublisher;
import be.kdg.airfreight.simulator.infrastructure.messaging.UldMessage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SimulatorServiceTest {
    private static final Instant NOW = Instant.parse("2026-01-02T08:00:00Z");

    private final RecordingPublisher publisher = new RecordingPublisher();
    private final SimulatorService simulator = new SimulatorService(publisher, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void oneSsmIsSentPerScheduleRecordInTheOrderOfTheRecords() {
        simulator.sendSchedules(List.of(scheduleFor("HJ221"), scheduleFor("3V11")));

        assertThat(publisher.ssms).extracting(SsmMessage::flightNumber).containsExactly("HJ221", "3V11");
    }

    @Test
    void oneUldMessageIsSentPerBookingRecordInTheOrderOfTheRecords() {
        simulator.sendBookings(List.of(bookingOf("AKE16689R7"), bookingOf("PMC12345KL"), bookingOf("AKE22222R7")));

        assertThat(publisher.uldMessages).extracting(UldMessage::uldCode)
            .containsExactly("AKE16689R7", "PMC12345KL", "AKE22222R7");
    }

    @Test
    void everyMessageGetsItsOwnIdAndTheIdsAreReturned() {
        List<UUID> ids = simulator.sendSchedules(List.of(scheduleFor("HJ221"), scheduleFor("HJ221")));

        assertThat(ids).hasSize(2).doesNotHaveDuplicates();
        assertThat(publisher.ssms).extracting(SsmMessage::messageId).containsExactlyElementsOf(ids);
    }

    @Test
    void aMessageIsStampedWithTheMomentItIsSent() {
        simulator.sendBookings(List.of(bookingOf("AKE16689R7")));

        assertThat(publisher.uldMessages).extracting(UldMessage::createdAt).containsExactly(NOW);
    }

    private static ScheduleRecord scheduleFor(String flightNumber) {
        return new ScheduleRecord(LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 18), Set.of(DayOfWeek.MONDAY),
            LocalTime.of(6, 30), LocalTime.of(9, 45), flightNumber, "BRU", "ATH", "SXBAA");
    }

    private static BookingRecord bookingOf(String uldCode) {
        return new BookingRecord("HJ221", LocalDate.of(2026, 1, 9), "BRU", uldCode, new BigDecimal("1000"), "KG",
            "ATH");
    }

    /** Keeps what was sent, instead of a broker. */
    private static class RecordingPublisher implements StreamPublisher {
        private final List<SsmMessage> ssms = new ArrayList<>();
        private final List<UldMessage> uldMessages = new ArrayList<>();

        @Override
        public void send(SsmMessage ssm) {
            ssms.add(ssm);
        }

        @Override
        public void send(UldMessage uldMessage) {
            uldMessages.add(uldMessage);
        }
    }
}
