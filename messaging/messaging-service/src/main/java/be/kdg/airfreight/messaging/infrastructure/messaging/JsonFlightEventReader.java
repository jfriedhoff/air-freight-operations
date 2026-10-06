package be.kdg.airfreight.messaging.infrastructure.messaging;

import be.kdg.airfreight.messaging.domain.AircraftRegistration;
import be.kdg.airfreight.messaging.domain.AirportCode;
import be.kdg.airfreight.messaging.domain.FlightId;
import be.kdg.airfreight.messaging.domain.FlightNumber;
import be.kdg.airfreight.messaging.domain.FormatVersion;
import be.kdg.airfreight.messaging.domain.MessageId;
import be.kdg.airfreight.messaging.domain.flight.FlightEvent;
import be.kdg.airfreight.messaging.domain.receivedmessage.MessageReader;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
class JsonFlightEventReader implements MessageReader<FlightEvent> {
    private final JsonMapper mapper;

    JsonFlightEventReader(JsonMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public FlightEvent read(String body) {
        return JsonReading.read(mapper, body, FlightEventJson.class, JsonFlightEventReader::toDomain);
    }

    private static FlightEvent toDomain(FlightEventJson json) {
        return new FlightEvent(new MessageId(json.messageId()),
            new FormatVersion(JsonReading.required(json.version(), "version")),
            MessageTypes.kindOf(JsonReading.required(json.type(), "type")),
            new FlightId(new FlightNumber(json.flightNumber()), json.scheduledDepartureDate(),
                new AirportCode(json.departureAirport())),
            json.flightId(),
            new AirportCode(json.arrivalAirport()),
            new AircraftRegistration(json.registration()),
            json.std(), json.sta(), json.etd(), json.eta(), json.atd(), json.ata(),
            JsonReading.required(json.flightVersion(), "flightVersion"),
            json.occurredOn());
    }
}
