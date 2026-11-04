package be.kdg.airfreight.messaging.infrastructure.messaging;

import be.kdg.airfreight.messaging.domain.AircraftRegistration;
import be.kdg.airfreight.messaging.domain.AirportCatalog;
import be.kdg.airfreight.messaging.domain.FlightNumber;
import be.kdg.airfreight.messaging.domain.MessageId;
import be.kdg.airfreight.messaging.domain.receivedmessage.MessageReader;
import be.kdg.airfreight.messaging.domain.schedule.FlightSeries;
import be.kdg.airfreight.messaging.domain.schedule.SchedulePeriod;
import be.kdg.airfreight.messaging.domain.schedule.Ssm;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
class JsonSsmReader implements MessageReader<Ssm> {
    private final JsonMapper mapper;
    private final AirportCatalog airports;

    JsonSsmReader(JsonMapper mapper, AirportCatalog airports) {
        this.mapper = mapper;
        this.airports = airports;
    }

    @Override
    public Ssm read(String body) {
        return JsonReading.read(mapper, body, SsmJson.class, this::toDomain);
    }

    private Ssm toDomain(SsmJson json) {
        FlightSeries series = new FlightSeries(
            new SchedulePeriod(JsonReading.required(json.periodFrom(), "periodFrom"),
                JsonReading.required(json.periodTo(), "periodTo")),
            JsonReading.required(json.weekdays(), "weekdays"),
            JsonReading.required(json.departureTime(), "departureTime"),
            JsonReading.required(json.arrivalTime(), "arrivalTime"),
            new FlightNumber(JsonReading.required(json.flightNumber(), "flightNumber")),
            airports.airport(JsonReading.required(json.departureAirport(), "departureAirport")),
            airports.airport(JsonReading.required(json.arrivalAirport(), "arrivalAirport")),
            new AircraftRegistration(JsonReading.required(json.registration(), "registration")));
        return new Ssm(new MessageId(json.messageId()), JsonReading.knownVersion(json.version()), series);
    }
}
