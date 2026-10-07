package be.kdg.airfreight.messaging.infrastructure.messaging;

import java.util.UUID;

import be.kdg.airfreight.messaging.domain.AircraftRegistration;
import be.kdg.airfreight.messaging.domain.AirportCode;
import be.kdg.airfreight.messaging.domain.DomainRuleViolationException;
import be.kdg.airfreight.messaging.domain.FlightId;
import be.kdg.airfreight.messaging.domain.FlightNumber;
import be.kdg.airfreight.messaging.domain.FormatVersion;
import be.kdg.airfreight.messaging.domain.MessageId;
import be.kdg.airfreight.messaging.domain.flight.ArrivalEstimateChanged;
import be.kdg.airfreight.messaging.domain.flight.DepartureEstimateChanged;
import be.kdg.airfreight.messaging.domain.flight.FlightAnnounced;
import be.kdg.airfreight.messaging.domain.flight.FlightDeparted;
import be.kdg.airfreight.messaging.domain.flight.FlightEvent;
import be.kdg.airfreight.messaging.domain.flight.FlightLanded;
import be.kdg.airfreight.messaging.domain.flight.FlightRemoved;
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

    /** The type decides which event it is; each event then insists on the fields it needs. */
    private static FlightEvent toDomain(FlightEventJson json) {
        MessageId id = new MessageId(json.messageId());
        FormatVersion version = new FormatVersion(JsonReading.required(json.version(), "version"));
        FlightId flight = new FlightId(new FlightNumber(json.flightNumber()), json.scheduledDepartureDate(),
            new AirportCode(json.departureAirport()));
        UUID reference = json.flightId();
        String type = JsonReading.required(json.type(), "type");
        return switch (MessageTypes.flightEventNameOf(type)) {
            case MessageTypes.ANNOUNCED -> new FlightAnnounced(id, version, flight, reference,
                arrivalAirportOf(json), registrationOf(json), json.std(), json.sta(), json.occurredOn());
            case MessageTypes.DEPARTURE_ESTIMATE_CHANGED -> new DepartureEstimateChanged(id, version, flight,
                reference, registrationOf(json), json.etd(), json.occurredOn());
            case MessageTypes.ARRIVAL_ESTIMATE_CHANGED -> new ArrivalEstimateChanged(id, version, flight,
                reference, arrivalAirportOf(json), registrationOf(json), json.eta(), json.occurredOn());
            case MessageTypes.DEPARTED -> new FlightDeparted(id, version, flight, reference,
                registrationOf(json), json.atd(), json.occurredOn());
            case MessageTypes.LANDED -> new FlightLanded(id, version, flight, reference,
                arrivalAirportOf(json), registrationOf(json), json.ata(), json.occurredOn());
            case MessageTypes.REMOVED -> new FlightRemoved(id, version, flight, reference, json.occurredOn());
            default -> throw new DomainRuleViolationException("Unknown flight event type '" + type + "'");
        };
    }

    private static AirportCode arrivalAirportOf(FlightEventJson json) {
        return new AirportCode(JsonReading.required(json.arrivalAirport(), "arrivalAirport"));
    }

    private static AircraftRegistration registrationOf(FlightEventJson json) {
        return new AircraftRegistration(JsonReading.required(json.registration(), "registration"));
    }
}
