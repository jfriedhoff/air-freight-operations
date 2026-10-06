package be.kdg.airfreight.messaging.infrastructure.messaging;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import be.kdg.airfreight.messaging.domain.flight.FlightEvent;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Contract flight-operations.flight.*, version 1, and messaging.flight.* when Messaging passes it on:
 * the same fields, a new messageId and type. Every flight event carries the whole flight.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
record FlightEventJson(UUID messageId, String type, Integer version, Instant occurredOn, UUID flightId,
                       String flightNumber, LocalDate scheduledDepartureDate, String departureAirport,
                       String arrivalAirport, String registration, Instant std, Instant sta, Instant etd,
                       Instant eta, Instant atd, Instant ata, Long flightVersion) {

    static FlightEventJson from(FlightEvent event, UUID messageId, String type) {
        return new FlightEventJson(messageId, type, MessageTypes.VERSION, event.occurredOn(),
            event.flightReference(), event.flightId().flightNumber().value(),
            event.flightId().scheduledDepartureDate(), event.flightId().departureAirport().value(),
            event.arrivalAirport().value(), event.registration().value(), event.std(), event.sta(),
            event.etd(), event.eta(), event.atd(), event.ata(), event.flightVersion());
    }
}
