package be.kdg.airfreight.messaging.infrastructure.messaging;

import java.time.Instant;

import be.kdg.airfreight.messaging.domain.FlightId;
import be.kdg.airfreight.messaging.domain.FormatVersion;
import be.kdg.airfreight.messaging.domain.MessageId;
import be.kdg.airfreight.messaging.domain.MessageType;
import be.kdg.airfreight.messaging.domain.flight.FlightEvent;
import be.kdg.airfreight.messaging.domain.flight.FlightEventKind;
import be.kdg.airfreight.messaging.domain.outgoingmessage.MessageWriter;
import be.kdg.airfreight.messaging.domain.outgoingmessage.OutgoingMessage;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** FLM: a flight was created. */
@Component
class FlmWriter implements MessageWriter<FlightEvent> {
    private static final FormatVersion FORMAT_VERSION = new FormatVersion(1);

    private final JsonMapper mapper;

    FlmWriter(JsonMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public boolean writes(FlightEvent event) {
        return event.kind() == FlightEventKind.ANNOUNCED;
    }

    @Override
    public OutgoingMessage write(FlightEvent event, MessageId id, Instant createdAt) {
        FlightId flight = event.flightId();
        String body = mapper.writeValueAsString(new FlmJson(id.id(), createdAt, FORMAT_VERSION.number(),
            flight.flightNumber().value(), flight.scheduledDepartureDate(), event.std(), event.sta(),
            flight.departureAirport().value(), event.arrivalAirport().value()));
        return OutgoingMessage.create(id, flight, MessageType.FLM, FORMAT_VERSION, createdAt, body);
    }
}
