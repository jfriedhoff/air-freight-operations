package be.kdg.airfreight.messaging.infrastructure.persistence;

import be.kdg.airfreight.messaging.domain.FlightId;
import be.kdg.airfreight.messaging.domain.MessageId;
import be.kdg.airfreight.messaging.domain.receivedmessage.ReceivedMessage;
import be.kdg.airfreight.messaging.domain.receivedmessage.ReceivedMessageRepository;
import be.kdg.airfreight.messaging.infrastructure.persistence.jpa.JpaReceivedMessage;
import be.kdg.airfreight.messaging.infrastructure.persistence.jpa.JpaReceivedMessageRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Profile("db")
class DbReceivedMessageRepository implements ReceivedMessageRepository {
    private final JpaReceivedMessageRepository jpaMessages;

    DbReceivedMessageRepository(JpaReceivedMessageRepository jpaMessages) {
        this.jpaMessages = jpaMessages;
    }

    @Override
    public void save(ReceivedMessage message) {
        jpaMessages.save(JpaReceivedMessage.fromDomain(message));
    }

    @Override
    public boolean existsById(MessageId id) {
        return jpaMessages.existsById(id.id());
    }

    @Override
    @Transactional
    public void deleteByFlight(FlightId flightId) {
        jpaMessages.deleteByFlightNumberAndScheduledDepartureDateAndDepartureAirport(
            flightId.flightNumber().value(), flightId.scheduledDepartureDate(),
            flightId.departureAirport().value());
    }
}
