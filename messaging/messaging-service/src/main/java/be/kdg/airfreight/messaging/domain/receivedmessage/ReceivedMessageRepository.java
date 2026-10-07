package be.kdg.airfreight.messaging.domain.receivedmessage;

import be.kdg.airfreight.messaging.domain.FlightId;
import be.kdg.airfreight.messaging.domain.MessageId;

public interface ReceivedMessageRepository {
    void save(ReceivedMessage message);

    boolean existsById(MessageId id);

    void deleteByFlight(FlightId flightId);
}
