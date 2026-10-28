package be.kdg.airfreight.messaging.infrastructure.messaging;

import java.time.Instant;

import be.kdg.airfreight.messaging.domain.FlightId;
import be.kdg.airfreight.messaging.domain.FormatVersion;
import be.kdg.airfreight.messaging.domain.MessageId;
import be.kdg.airfreight.messaging.domain.MessageType;
import be.kdg.airfreight.messaging.domain.load.LoadFinalized;
import be.kdg.airfreight.messaging.domain.outgoingmessage.MessageWriter;
import be.kdg.airfreight.messaging.domain.outgoingmessage.OutgoingMessage;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** CPM: a load was finalized. */
@Component
class CpmWriter implements MessageWriter<LoadFinalized> {
    private static final FormatVersion FORMAT_VERSION = new FormatVersion(1);

    private final JsonMapper mapper;

    CpmWriter(JsonMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public boolean writes(LoadFinalized event) {
        return true;
    }

    @Override
    public OutgoingMessage write(LoadFinalized event, MessageId id, Instant createdAt) {
        FlightId flight = event.flightId();
        String body = mapper.writeValueAsString(new CpmJson(id.id(), createdAt, FORMAT_VERSION.number(), "CPM",
            flight.flightNumber().value(), flight.scheduledDepartureDate(), flight.departureAirport().value(),
            event.load().stream().map(CpmUldJson::from).toList()));
        return OutgoingMessage.create(id, flight, MessageType.CPM, FORMAT_VERSION, createdAt, body);
    }
}
