package be.kdg.airfreight.messaging.infrastructure.messaging;

import be.kdg.airfreight.messaging.domain.AirportCatalog;
import be.kdg.airfreight.messaging.domain.FlightNumber;
import be.kdg.airfreight.messaging.domain.FormatVersion;
import be.kdg.airfreight.messaging.domain.MessageId;
import be.kdg.airfreight.messaging.domain.booking.UldCode;
import be.kdg.airfreight.messaging.domain.booking.UldMessage;
import be.kdg.airfreight.messaging.domain.booking.WeightUnit;
import be.kdg.airfreight.messaging.domain.receivedmessage.MessageReader;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
class JsonUldMessageReader implements MessageReader<UldMessage> {
    private final JsonMapper mapper;
    private final AirportCatalog airports;

    JsonUldMessageReader(JsonMapper mapper, AirportCatalog airports) {
        this.mapper = mapper;
        this.airports = airports;
    }

    @Override
    public UldMessage read(String body) {
        return JsonReading.read(mapper, body, UldMessageJson.class, this::toDomain);
    }

    private UldMessage toDomain(UldMessageJson json) {
        return new UldMessage(new MessageId(json.messageId()),
            new FormatVersion(JsonReading.required(json.version(), "version")),
            new FlightNumber(json.flightNumber()),
            JsonReading.required(json.dayOfMonth(), "dayOfMonth"),
            airports.airport(json.departureAirport()),
            new UldCode(json.uldCode()),
            json.weight(),
            WeightUnit.of(json.weightUnit()),
            airports.airport(json.destination()));
    }
}
