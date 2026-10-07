package be.kdg.airfreight.messaging.infrastructure.persistence;

import java.util.List;

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
