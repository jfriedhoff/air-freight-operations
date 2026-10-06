package be.kdg.airfreight.messaging.infrastructure.messaging;

import be.kdg.airfreight.messaging.domain.AirportCode;
import be.kdg.airfreight.messaging.domain.FlightNumber;
import be.kdg.airfreight.messaging.domain.FormatVersion;
import be.kdg.airfreight.messaging.domain.MessageId;
import be.kdg.airfreight.messaging.domain.booking.UldMessage;
import be.kdg.airfreight.messaging.domain.receivedmessage.MessageReader;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
class JsonUldMessageReader implements MessageReader<UldMessage> {
    private final JsonMapper mapper;

    JsonUldMessageReader(JsonMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public UldMessage read(String body) {
        return JsonReading.read(mapper, body, UldMessageJson.class, JsonUldMessageReader::toDomain);
    }

    private static UldMessage toDomain(UldMessageJson json) {
        return new UldMessage(new MessageId(json.messageId()),
            new FormatVersion(JsonReading.required(json.version(), "version")),
            new FlightNumber(json.flightNumber()),
            JsonReading.required(json.dayOfMonth(), "dayOfMonth"),
            new AirportCode(json.departureAirport()),
            json.uldCode(),
            json.weight(),
            json.weightUnit(),
            new AirportCode(json.destination()));
    }
}
