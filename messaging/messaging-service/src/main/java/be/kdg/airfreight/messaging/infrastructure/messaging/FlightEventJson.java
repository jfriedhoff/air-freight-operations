package be.kdg.airfreight.messaging.infrastructure.messaging;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import be.kdg.airfreight.messaging.domain.AircraftRegistration;
import be.kdg.airfreight.messaging.domain.AirportCode;
import be.kdg.airfreight.messaging.domain.flight.ArrivalEstimateChanged;
import be.kdg.airfreight.messaging.domain.flight.DepartureEstimateChanged;
import be.kdg.airfreight.messaging.domain.flight.FlightAnnounced;
import be.kdg.airfreight.messaging.domain.flight.FlightDeparted;
import be.kdg.airfreight.messaging.domain.flight.FlightEvent;
import be.kdg.airfreight.messaging.domain.flight.FlightLanded;
import be.kdg.airfreight.messaging.domain.flight.FlightRemoved;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.jspecify.annotations.Nullable;

/**
 * Contracts flight-operations.flight.*, version 1, and messaging.flight.* when Messaging passes an event on.
 * Every event has the first eight fields; of the others it has only those that belong to its type.
 * A field that an event does not have is left out of the JSON.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
record FlightEventJson(UUID messageId, String type, Integer version, Instant occurredOn, UUID flightId,
                       String flightNumber, LocalDate scheduledDepartureDate, String departureAirport,
                       @Nullable String arrivalAirport, @Nullable String registration, @Nullable Instant std,
                       @Nullable Instant sta, @Nullable Instant etd, @Nullable Instant eta, @Nullable Instant atd,
                       @Nullable Instant ata) {

    /** The same event under a new messageId and type, for passing it on. */
    static FlightEventJson from(FlightEvent event, UUID messageId, String type) {
        return switch (event) {
            case FlightAnnounced e -> of(e, messageId, type, e.arrivalAirport(), e.registration(),
                e.std(), e.sta(), null, null, null, null);
            case DepartureEstimateChanged e -> of(e, messageId, type, null, e.registration(),
                null, null, e.etd(), null, null, null);
            case ArrivalEstimateChanged e -> of(e, messageId, type, e.arrivalAirport(), e.registration(),
                null, null, null, e.eta(), null, null);
            case FlightDeparted e -> of(e, messageId, type, null, e.registration(),
                null, null, null, null, e.atd(), null);
            case FlightLanded e -> of(e, messageId, type, e.arrivalAirport(), e.registration(),
                null, null, null, null, null, e.ata());
            case FlightRemoved e -> of(e, messageId, type, null, null, null, null, null, null, null, null);
        };
    }

    private static FlightEventJson of(FlightEvent event, UUID messageId, String type,
                                      @Nullable AirportCode arrivalAirport,
                                      @Nullable AircraftRegistration registration, @Nullable Instant std,
                                      @Nullable Instant sta, @Nullable Instant etd, @Nullable Instant eta,
                                      @Nullable Instant atd, @Nullable Instant ata) {
        return new FlightEventJson(messageId, type, MessageTypes.VERSION, event.occurredOn(),
            event.flightReference(), event.flightId().flightNumber().value(),
            event.flightId().scheduledDepartureDate(), event.flightId().departureAirport().value(),
            arrivalAirport == null ? null : arrivalAirport.value(),
            registration == null ? null : registration.value(), std, sta, etd, eta, atd, ata);
    }
}
