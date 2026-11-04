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
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The partner relay reads the unsent messages, sends one and then stores that it was sent. A FlightRemoved can
 * delete the message in between. These tests take those steps one after the other, without threads. The row is
 * stored as sent from the start, so the partner relay that runs every second in the tests never picks it up.
 */
@SpringBootTest
class OutgoingMessageRemovalTest {
    private static final FlightId FLIGHT =
        new FlightId(new FlightNumber("HJ998"), LocalDate.of(2026, 1, 9), new AirportCode("BRU"));
    private static final Instant CREATED = Instant.parse("2026-01-09T07:00:00Z");

    @Autowired
    private OutgoingMessageRepository messages;
    @Autowired
    private JpaOutgoingMessageRepository jpaMessages;

    @AfterEach
    void cleanUp() {
        messages.deleteByFlight(FLIGHT);
    }

    @Test
    void aMessageWhoseFlightWasRemovedWhileItWasSentStaysRemoved() {
        MessageId id = MessageId.create();
        messages.save(flm(id, Instant.parse("2026-01-09T07:00:01Z")));
        OutgoingMessage readByTheRelay = flm(id, null);

        messages.deleteByFlight(FLIGHT);
        readByTheRelay.markSent(Instant.parse("2026-01-09T07:00:02Z"));
        messages.saveSent(readByTheRelay);

        assertThat(jpaMessages.findById(id.id())).isEmpty();
    }

    @Test
    void aMessageThatStillExistsIsStoredAsSent() {
        MessageId id = MessageId.create();
        messages.save(flm(id, Instant.parse("2026-01-09T07:00:01Z")));
        OutgoingMessage readByTheRelay = flm(id, null);
        Instant sentAt = Instant.parse("2026-01-09T07:00:02Z");

        readByTheRelay.markSent(sentAt);
        messages.saveSent(readByTheRelay);

        assertThat(jpaMessages.findById(id.id())).map(JpaOutgoingMessage::toDomain)
            .map(OutgoingMessage::getSentAt).contains(sentAt);
    }

    private static OutgoingMessage flm(MessageId id, @Nullable Instant sentAt) {
        return OutgoingMessage.hydrate(id, FLIGHT, MessageType.FLM, new FormatVersion(1), CREATED, "FLM", sentAt);
    }
}
