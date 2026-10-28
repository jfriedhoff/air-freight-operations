package be.kdg.airfreight.messaging.domain.load;

import be.kdg.airfreight.messaging.domain.AirportCode;
import be.kdg.airfreight.messaging.domain.FlightId;
import be.kdg.airfreight.messaging.domain.FlightNumber;
import be.kdg.airfreight.messaging.domain.FormatVersion;
import be.kdg.airfreight.messaging.domain.MessageId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The event LoadService publishes once the CPM is written, so Flight Operations can accept the ATD. The rules of
 * a CPM line are checked by the @unit scenario of story 25.
 */
class LoadFinalizedTest {
    private static final FlightId HJ221 =
        new FlightId(new FlightNumber("HJ221"), LocalDate.of(2026, 1, 9), new AirportCode("BRU"));
    private static final Instant NOW = Instant.parse("2026-01-09T05:30:00Z");

    @Test
    void writingTheCpmIsAnnouncedForTheSameFlight() {
        LoadFinalized load = new LoadFinalized(MessageId.create(), new FormatVersion(1), HJ221,
            List.of(new CpmLine("AKE16689R7", "CL", new BigDecimal("1250"), "KG", new AirportCode("ATH"))), NOW);

        CpmCreated event = load.cpmCreated(NOW);

        assertThat(event.flightId()).isEqualTo(HJ221);
        assertThat(event.occurredOn()).isEqualTo(NOW);
    }
}
