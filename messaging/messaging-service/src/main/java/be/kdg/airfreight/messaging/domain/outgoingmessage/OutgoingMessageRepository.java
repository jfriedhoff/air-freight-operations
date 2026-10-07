package be.kdg.airfreight.messaging.domain.outgoingmessage;

import java.util.List;

import be.kdg.airfreight.messaging.domain.FlightId;

public interface OutgoingMessageRepository {
    void save(OutgoingMessage message);

    List<OutgoingMessage> findUnsentOldestFirst();

    void deleteByFlight(FlightId flightId);
}
