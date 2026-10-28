package be.kdg.airfreight.messaging.domain.telegram;

import be.kdg.airfreight.messaging.domain.AircraftRegistration;
import be.kdg.airfreight.messaging.domain.AirportCode;
import be.kdg.airfreight.messaging.domain.FlightId;
import be.kdg.airfreight.messaging.domain.FlightNumber;
import be.kdg.airfreight.messaging.domain.FormatVersion;
import be.kdg.airfreight.messaging.domain.MessageId;
import be.kdg.airfreight.messaging.domain.MessageType;
import be.kdg.airfreight.messaging.domain.flight.ArrivalEstimateChanged;
import be.kdg.airfreight.messaging.domain.flight.DepartureEstimateChanged;
import be.kdg.airfreight.messaging.domain.flight.FlightDeparted;
import be.kdg.airfreight.messaging.domain.flight.FlightEvent;
import be.kdg.airfreight.messaging.domain.flight.FlightLanded;
import be.kdg.airfreight.messaging.domain.outgoingmessage.OutgoingMessage;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What FlightEventService relies on when it turns a flight event into a partner message. The telegram text
 * itself is checked by the @unit scenarios of stories 11 and 14.
 */
class MvtWriterTest {
    private static final FormatVersion VERSION = new FormatVersion(1);
    private static final AirportCode ATH = new AirportCode("ATH");
    private static final FlightId HJ221 =
        new FlightId(new FlightNumber("HJ221"), LocalDate.of(2026, 1, 9), new AirportCode("BRU"));
    private static final AircraftRegistration SXBAA = new AircraftRegistration("SXBAA");
    private static final Instant CREATED_AT = Instant.parse("2026-01-09T05:00:00Z");

    /** FlightEventService asks every writer whether it writes an event, so each writer must only say yes to its own. */
    @Test
    void eachWriterOnlyWritesItsOwnEvent() {
        FlightEvent etd = new DepartureEstimateChanged(MessageId.create(), VERSION, HJ221, UUID.randomUUID(), SXBAA,
            Instant.parse("2026-01-09T06:45:00Z"), CREATED_AT);
        FlightEvent eta = new ArrivalEstimateChanged(MessageId.create(), VERSION, HJ221, UUID.randomUUID(), ATH,
            SXBAA, Instant.parse("2026-01-09T10:00:00Z"), CREATED_AT);
        FlightEvent atd = new FlightDeparted(MessageId.create(), VERSION, HJ221, UUID.randomUUID(), SXBAA,
            Instant.parse("2026-01-09T06:52:00Z"), CREATED_AT);
        FlightEvent ata = new FlightLanded(MessageId.create(), VERSION, HJ221, UUID.randomUUID(), ATH, SXBAA,
            Instant.parse("2026-01-09T10:04:00Z"), CREATED_AT);

        assertThat(new EdWriter().writes(etd)).isTrue();
        assertThat(new EdWriter().writes(eta)).isFalse();
        assertThat(new EaWriter().writes(eta)).isTrue();
        assertThat(new EaWriter().writes(etd)).isFalse();
        assertThat(new AdWriter().writes(atd)).isTrue();
        assertThat(new AdWriter().writes(ata)).isFalse();
        assertThat(new AaWriter().writes(ata)).isTrue();
        assertThat(new AaWriter().writes(atd)).isFalse();
    }

    /** PartnerMessageRelay sends the message later, so it must keep the id, the flight and the time it was written. */
    @Test
    void aWrittenMessageIsReadyToBeSent() {
        MessageId id = MessageId.create();
        OutgoingMessage message = new EdWriter().write(new DepartureEstimateChanged(MessageId.create(), VERSION,
            HJ221, UUID.randomUUID(), SXBAA, Instant.parse("2026-01-09T06:45:00Z"), CREATED_AT), id, CREATED_AT);

        assertThat(message.getId()).isEqualTo(id);
        assertThat(message.getFlightId()).isEqualTo(HJ221);
        assertThat(message.getType()).isEqualTo(MessageType.MVT_ED);
        assertThat(message.getFormatVersion()).isEqualTo(VERSION);
        assertThat(message.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(message.isSent()).isFalse();
        assertThat(message.getSentAt()).isNull();
    }
}
