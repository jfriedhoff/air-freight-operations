package be.kdg.airfreight.messaging.infrastructure.messaging;

import java.time.Instant;

import be.kdg.airfreight.messaging.domain.FlightId;
import be.kdg.airfreight.messaging.domain.FormatVersion;
import be.kdg.airfreight.messaging.domain.MessageId;
import be.kdg.airfreight.messaging.domain.MessageType;
import be.kdg.airfreight.messaging.domain.flight.FlightAnnounced;
import be.kdg.airfreight.messaging.domain.flight.FlightEvent;
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
        return event instanceof FlightAnnounced;
    }

    @Override
    public OutgoingMessage write(FlightEvent flightEvent, MessageId id, Instant createdAt) {
        FlightAnnounced event = (FlightAnnounced) flightEvent;
        FlightId flight = event.flightId();
        String body = mapper.writeValueAsString(new FlmJson(id.id(), createdAt, FORMAT_VERSION.number(), "FLM",
            flight.flightNumber().value(), flight.scheduledDepartureDate(), event.std(), event.sta(),
            flight.departureAirport().value(), event.arrivalAirport().value()));
        return OutgoingMessage.create(id, flight, MessageType.FLM, FORMAT_VERSION, createdAt, body);
    }
}
