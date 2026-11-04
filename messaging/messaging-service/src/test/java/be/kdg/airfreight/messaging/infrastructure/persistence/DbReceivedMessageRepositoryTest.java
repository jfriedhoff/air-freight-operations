package be.kdg.airfreight.messaging.infrastructure.persistence;

import be.kdg.airfreight.messaging.domain.AirportCode;
import be.kdg.airfreight.messaging.domain.FlightId;
import be.kdg.airfreight.messaging.domain.FlightNumber;
import be.kdg.airfreight.messaging.domain.FormatVersion;
import be.kdg.airfreight.messaging.domain.MessageId;
import be.kdg.airfreight.messaging.domain.MessageType;
import be.kdg.airfreight.messaging.domain.receivedmessage.ReceivedMessage;
import be.kdg.airfreight.messaging.domain.receivedmessage.ReceivedMessageRepository;
import be.kdg.airfreight.messaging.infrastructure.persistence.jpa.JpaReceivedMessage;
import be.kdg.airfreight.messaging.infrastructure.persistence.jpa.JpaReceivedMessageRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DbReceivedMessageRepositoryTest {
    private static final LocalDate DATE = LocalDate.of(2026, 1, 9);
    private static final FlightId FLIGHT = new FlightId(new FlightNumber("ABC1234"), DATE, new AirportCode("BRU"));

    @Autowired
    private ReceivedMessageRepository messages;
    @Autowired
    private JpaReceivedMessageRepository jpaMessages;

    @AfterEach
    void cleanUp() {
        messages.deleteByFlight(FLIGHT);
    }

    /** Seven characters (a three-letter airline code and four digits) is the longest flight number. */
    @Test
    void aUldMessageForAFlightNumberOfSevenCharactersIsSavedWithItsFlight() {
        MessageId id = MessageId.create();
        messages.save(ReceivedMessage.processed(id, MessageType.ULD, new FormatVersion(1),
            Instant.parse("2026-01-08T10:00:00Z"), "{}", FLIGHT, DATE));

        assertThat(jpaMessages.findById(id.id())).map(JpaReceivedMessage::toDomain)
            .map(ReceivedMessage::getFlightId).contains(FLIGHT);
    }
}
