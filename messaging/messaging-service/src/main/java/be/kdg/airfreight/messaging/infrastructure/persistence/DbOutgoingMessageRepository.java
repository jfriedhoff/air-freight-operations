package be.kdg.airfreight.messaging.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import be.kdg.airfreight.messaging.domain.FlightId;
import be.kdg.airfreight.messaging.domain.outgoingmessage.OutgoingMessage;
import be.kdg.airfreight.messaging.domain.outgoingmessage.OutgoingMessageRepository;
import be.kdg.airfreight.messaging.infrastructure.persistence.jpa.JpaOutgoingMessage;
import be.kdg.airfreight.messaging.infrastructure.persistence.jpa.JpaOutgoingMessageRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Profile("db")
class DbOutgoingMessageRepository implements OutgoingMessageRepository {
    private final JpaOutgoingMessageRepository jpaMessages;

    DbOutgoingMessageRepository(JpaOutgoingMessageRepository jpaMessages) {
        this.jpaMessages = jpaMessages;
    }

    @Override
    public void save(OutgoingMessage message) {
        jpaMessages.save(JpaOutgoingMessage.fromDomain(message));
    }

    /**
     * An update, not a save: save is a merge, and a merge inserts the row again when a FlightRemoved deleted it
     * between reading the unsent messages and sending this one.
     */
    @Override
    @Transactional
    public void saveSent(OutgoingMessage message) {
        Instant sentAt = Objects.requireNonNull(message.getSentAt(), "Only a sent message can be saved as sent");
        jpaMessages.markSent(message.getId().id(), sentAt);
    }

    @Override
    public List<OutgoingMessage> findUnsentOldestFirst() {
        return jpaMessages.findBySentAtIsNullOrderByCreatedAtAsc().stream()
            .map(JpaOutgoingMessage::toDomain)
            .toList();
    }

    @Override
    @Transactional
    public void deleteByFlight(FlightId flightId) {
        jpaMessages.deleteByFlightNumberAndScheduledDepartureDateAndDepartureAirport(
            flightId.flightNumber().value(), flightId.scheduledDepartureDate(),
            flightId.departureAirport().value());
    }
}
