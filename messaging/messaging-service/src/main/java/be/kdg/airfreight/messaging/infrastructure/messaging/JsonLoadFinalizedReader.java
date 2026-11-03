package be.kdg.airfreight.messaging.infrastructure.messaging;

import be.kdg.airfreight.messaging.domain.AirportCode;
import be.kdg.airfreight.messaging.domain.FlightId;
import be.kdg.airfreight.messaging.domain.FlightNumber;
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
            JsonReading.knownVersion(json.version()),
            new FlightId(new FlightNumber(JsonReading.required(json.flightNumber(), "flightNumber")),
                JsonReading.required(json.scheduledDepartureDate(), "scheduledDepartureDate"),
                new AirportCode(JsonReading.required(json.departureAirport(), "departureAirport"))),
            JsonReading.required(json.load(), "load").stream()
                .map(JsonLoadFinalizedReader::toCpmLine)
                .toList(),
            JsonReading.required(json.occurredOn(), "occurredOn"));
    }

    private static CpmLine toCpmLine(UldLineJson line) {
        return new CpmLine(JsonReading.required(line.uldCode(), "load[].uldCode"),
            JsonReading.required(line.position(), "load[].position"),
            JsonReading.required(line.weight(), "load[].weight"),
            JsonReading.required(line.weightUnit(), "load[].weightUnit"),
            new AirportCode(JsonReading.required(line.destination(), "load[].destination")));
    }
}
