package be.kdg.airfreight.messaging.infrastructure.messaging;

import be.kdg.airfreight.messaging.domain.AirportCode;
import be.kdg.airfreight.messaging.domain.FlightId;
import be.kdg.airfreight.messaging.domain.FlightNumber;
import be.kdg.airfreight.messaging.domain.FormatVersion;
import be.kdg.airfreight.messaging.domain.MessageId;
import be.kdg.airfreight.messaging.domain.load.CpmLine;
import be.kdg.airfreight.messaging.domain.load.LoadFinalized;
import be.kdg.airfreight.messaging.domain.receivedmessage.MessageReader;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
class JsonLoadFinalizedReader implements MessageReader<LoadFinalized> {
    private final JsonMapper mapper;

    JsonLoadFinalizedReader(JsonMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public LoadFinalized read(String body) {
        return JsonReading.read(mapper, body, LoadFinalizedJson.class, JsonLoadFinalizedReader::toDomain);
    }

    private static LoadFinalized toDomain(LoadFinalizedJson json) {
        return new LoadFinalized(new MessageId(json.messageId()),
            new FormatVersion(JsonReading.required(json.version(), "version")),
            new FlightId(new FlightNumber(json.flightNumber()), json.scheduledDepartureDate(),
                new AirportCode(json.departureAirport())),
            JsonReading.required(json.load(), "load").stream()
                .map(line -> new CpmLine(line.uldCode(), line.position(), line.weight(), line.weightUnit(),
                    new AirportCode(line.destination())))
                .toList(),
            json.occurredOn());
    }
}
