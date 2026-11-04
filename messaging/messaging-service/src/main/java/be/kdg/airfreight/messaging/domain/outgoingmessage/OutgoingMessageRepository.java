package be.kdg.airfreight.messaging.domain.outgoingmessage;

import java.util.List;

import be.kdg.airfreight.messaging.domain.FlightId;

public interface OutgoingMessageRepository {
    void save(OutgoingMessage message);

    /**
     * Stores that a message was sent. A message whose flight was removed in the meantime stays removed: it is not
     * stored again.
     */
    void saveSent(OutgoingMessage message);

    List<OutgoingMessage> findUnsentOldestFirst();

    void deleteByFlight(FlightId flightId);
}
