package be.kdg.airfreight.messaging.infrastructure.persistence;

import be.kdg.airfreight.messaging.domain.AirportCode;
import be.kdg.airfreight.messaging.domain.FlightId;
import be.kdg.airfreight.messaging.domain.FlightNumber;
import be.kdg.airfreight.messaging.domain.FormatVersion;
import be.kdg.airfreight.messaging.domain.MessageId;
import be.kdg.airfreight.messaging.domain.MessageType;
import be.kdg.airfreight.messaging.domain.outgoingmessage.OutgoingMessage;
import be.kdg.airfreight.messaging.domain.outgoingmessage.OutgoingMessageRepository;
import be.kdg.airfreight.messaging.infrastructure.persistence.jpa.JpaOutgoingMessage;
import be.kdg.airfreight.messaging.infrastructure.persistence.jpa.JpaOutgoingMessageRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DbOutgoingMessageRepositoryTest {
    private static final FlightId FLIGHT =
        new FlightId(new FlightNumber("ABC1234"), LocalDate.of(2026, 1, 9), new AirportCode("BRU"));

    @Autowired
    private OutgoingMessageRepository messages;
    @Autowired
    private JpaOutgoingMessageRepository jpaMessages;

    @AfterEach
    void cleanUp() {
        messages.deleteByFlight(FLIGHT);
    }

    /**
     * Seven characters (a three-letter airline code and four digits) is the longest flight number. The message
     * is saved as sent, so the partner relay that runs every second in the tests does not send it.
     */
    @Test
    void anFlmForAFlightNumberOfSevenCharactersIsSavedWithItsFlight() {
        MessageId id = MessageId.create();
        OutgoingMessage flm = OutgoingMessage.create(id, FLIGHT, MessageType.FLM, new FormatVersion(1),
            Instant.parse("2026-01-09T07:00:00Z"), "FLM");
        flm.markSent(Instant.parse("2026-01-09T07:00:01Z"));
        messages.save(flm);

        assertThat(jpaMessages.findById(id.id())).map(JpaOutgoingMessage::toDomain)
            .map(OutgoingMessage::getFlightId).contains(FLIGHT);
    }
}
