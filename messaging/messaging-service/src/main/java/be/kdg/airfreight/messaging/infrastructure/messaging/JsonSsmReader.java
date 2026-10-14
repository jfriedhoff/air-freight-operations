package be.kdg.airfreight.messaging.infrastructure.messaging;

import be.kdg.airfreight.messaging.domain.AircraftRegistration;
import be.kdg.airfreight.messaging.domain.AirportCatalog;
import be.kdg.airfreight.messaging.domain.FlightNumber;
import be.kdg.airfreight.messaging.domain.FormatVersion;
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
            new SchedulePeriod(json.periodFrom(), json.periodTo()),
            JsonReading.required(json.weekdays(), "weekdays"),
            json.departureTime(),
            json.arrivalTime(),
            new FlightNumber(json.flightNumber()),
            airports.airport(json.departureAirport()),
            airports.airport(json.arrivalAirport()),
            new AircraftRegistration(json.registration()));
        return new Ssm(new MessageId(json.messageId()),
            new FormatVersion(JsonReading.required(json.version(), "version")), series);
    }
}
