package be.kdg.airfreight.simulator.infrastructure.messaging;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import be.kdg.airfreight.simulator.SentMessage;
import be.kdg.airfreight.simulator.StreamProbe;
import be.kdg.airfreight.simulator.application.BookingRecord;
import be.kdg.airfreight.simulator.application.ScheduleRecord;
import be.kdg.airfreight.simulator.application.StreamPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AmqpStreamPublisherTest {
    private static final Instant NOW = Instant.parse("2026-01-02T08:00:00Z");

    @Autowired
    private StreamPublisher publisher;

    @Autowired
    private StreamProbe probe;

    @BeforeEach
    void clear() {
        probe.clear();
    }

    @Test
    void anSsmTravelsAsJsonWithItsTypeAsRoutingKeyAndItsIdAsMessageId() throws Exception {
        UUID id = UUID.fromString("11111111-1111-4111-8111-111111111111");

        publisher.send(new ScheduleRecord(LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 18),
            Set.of(DayOfWeek.MONDAY), LocalTime.of(6, 30), LocalTime.of(9, 45), "HJ221", "BRU", "ATH", "SXBAA"),
            id, NOW);

        SentMessage sent = probe.nextSsm();
        assertThat(sent.routingKey()).isEqualTo("network-planning.ssm");
        assertThat(sent.contentType()).isEqualTo("application/json");
        assertThat(sent.messageId()).isEqualTo(id.toString());
        JSONAssert.assertEquals("""
            {
              "messageId": "11111111-1111-4111-8111-111111111111",
              "type": "network-planning.ssm",
              "version": 1,
              "createdAt": "2026-01-02T08:00:00Z",
              "periodFrom": "2026-01-05",
              "periodTo": "2026-01-18",
              "weekdays": ["MONDAY"],
              "departureTime": "06:30",
              "arrivalTime": "09:45",
              "flightNumber": "HJ221",
              "departureAirport": "BRU",
              "arrivalAirport": "ATH",
              "registration": "SXBAA"
            }
            """, sent.body(), true);
    }

    @Test
    void aUldMessageTravelsOnTheBookingsStream() throws Exception {
        UUID id = UUID.fromString("22222222-2222-4222-8222-222222222222");

        publisher.send(new BookingRecord("HJ221", LocalDate.of(2026, 1, 9), "BRU", "AKE16689R7",
            new BigDecimal("1250.5"), "KG", "ATH"), id, NOW);

        SentMessage sent = probe.nextUldMessage();
        assertThat(sent.routingKey()).isEqualTo("cargo-booking.uld");
        assertThat(sent.contentType()).isEqualTo("application/json");
        assertThat(sent.messageId()).isEqualTo(id.toString());
        JSONAssert.assertEquals("""
            {
              "messageId": "22222222-2222-4222-8222-222222222222",
              "type": "cargo-booking.uld",
              "version": 1,
              "createdAt": "2026-01-02T08:00:00Z",
              "flightNumber": "HJ221",
              "dayOfMonth": 9,
              "departureAirport": "BRU",
              "uldCode": "AKE16689R7",
              "weight": 1250.5,
              "weightUnit": "KG",
              "destination": "ATH"
            }
            """, sent.body(), true);
    }
}
