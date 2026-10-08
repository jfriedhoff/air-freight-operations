package be.kdg.airfreight.simulator.application;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import be.kdg.airfreight.simulator.infrastructure.messaging.SsmMessage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduleRecordTest {
    private static final UUID MESSAGE_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final Instant NOW = Instant.parse("2026-01-02T08:00:00Z");

    @Test
    void aScheduleRecordBecomesAnSsmWithTheSameSchedule() {
        ScheduleRecord record = new ScheduleRecord(LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 18),
            Set.of(DayOfWeek.MONDAY, DayOfWeek.THURSDAY), LocalTime.of(6, 30), LocalTime.of(9, 45),
            "HJ221", "BRU", "ATH", "SXBAA");

        SsmMessage ssm = record.toSsm(MESSAGE_ID, NOW);

        assertThat(ssm).isEqualTo(new SsmMessage(MESSAGE_ID, "network-planning.ssm", 1, NOW,
            LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 18), Set.of(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
            LocalTime.of(6, 30), LocalTime.of(9, 45), "HJ221", "BRU", "ATH", "SXBAA"));
    }
}
