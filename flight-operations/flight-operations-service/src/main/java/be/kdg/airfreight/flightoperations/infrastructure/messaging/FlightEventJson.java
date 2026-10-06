package be.kdg.airfreight.flightoperations.infrastructure.messaging;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import be.kdg.airfreight.flightoperations.domain.flight.AircraftRegistration;
import be.kdg.airfreight.flightoperations.domain.flight.AirportCode;
import be.kdg.airfreight.flightoperations.domain.flight.ArrivalEstimateChanged;
import be.kdg.airfreight.flightoperations.domain.flight.DepartureEstimateChanged;
import be.kdg.airfreight.flightoperations.domain.flight.FinalizedLoadReceived;
import be.kdg.airfreight.flightoperations.domain.flight.FlightAnnounced;
import be.kdg.airfreight.flightoperations.domain.flight.FlightDeparted;
import be.kdg.airfreight.flightoperations.domain.flight.FlightEvent;
import be.kdg.airfreight.flightoperations.domain.flight.FlightId;
import be.kdg.airfreight.flightoperations.domain.flight.FlightLanded;
import be.kdg.airfreight.flightoperations.domain.flight.FlightNumber;
import be.kdg.airfreight.flightoperations.domain.flight.FlightRemoved;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.jspecify.annotations.Nullable;

/**
 * Contracts flight-operations.flight.*, version 1: the published language of Flight Operations.
 * Every event says which flight it is about and carries only what changed; a field an event does not
 * have is left out. Other contexts read this JSON: change it only with a new version.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record FlightEventJson(UUID messageId, String type, int version, Instant occurredOn, UUID flightId,
                       String flightNumber, LocalDate scheduledDepartureDate, String departureAirport,
                       @Nullable String arrivalAirport, @Nullable String registration, @Nullable Instant std,
                       @Nullable Instant sta, @Nullable Instant etd, @Nullable Instant eta, @Nullable Instant atd,
                       @Nullable Instant ata) {
    static final int VERSION = 1;
    private static final String TYPE_PREFIX = "flight-operations.flight.";

    /** The message for a domain event, or nothing for an event that stays inside this context. */
    static Optional<FlightEventJson> from(FlightEvent event, UUID messageId) {
        return Optional.ofNullable(switch (event) {
            case FlightAnnounced e -> of("announced", messageId, e.occurredOn(), e.flightId(), e.flightNumber(),
                e.scheduledDepartureDate(), e.departureAirport(), e.arrivalAirport(), e.registration(),
                e.std(), e.sta(), null, null, null, null);
            case DepartureEstimateChanged e -> of("departure-estimate-changed", messageId, e.occurredOn(),
                e.flightId(), e.flightNumber(), e.scheduledDepartureDate(), e.departureAirport(), null,
                e.registration(), null, null, e.etd(), null, null, null);
            case ArrivalEstimateChanged e -> of("arrival-estimate-changed", messageId, e.occurredOn(),
                e.flightId(), e.flightNumber(), e.scheduledDepartureDate(), e.departureAirport(),
                e.arrivalAirport(), e.registration(), null, null, null, e.eta(), null, null);
            case FlightDeparted e -> of("departed", messageId, e.occurredOn(), e.flightId(), e.flightNumber(),
                e.scheduledDepartureDate(), e.departureAirport(), null, e.registration(), null, null, null,
                null, e.atd(), null);
            case FlightLanded e -> of("landed", messageId, e.occurredOn(), e.flightId(), e.flightNumber(),
                e.scheduledDepartureDate(), e.departureAirport(), e.arrivalAirport(), e.registration(), null,
                null, null, null, null, e.ata());
            case FlightRemoved e -> of("removed", messageId, e.occurredOn(), e.flightId(), e.flightNumber(),
                e.scheduledDepartureDate(), e.departureAirport(), null, null, null, null, null, null, null, null);
            case FinalizedLoadReceived e -> null;
        });
    }

    private static FlightEventJson of(String name, UUID messageId, Instant occurredOn, FlightId flightId,
                                      FlightNumber flightNumber, LocalDate scheduledDepartureDate,
                                      AirportCode departureAirport, @Nullable AirportCode arrivalAirport,
                                      @Nullable AircraftRegistration registration, @Nullable Instant std,
                                      @Nullable Instant sta, @Nullable Instant etd, @Nullable Instant eta,
                                      @Nullable Instant atd, @Nullable Instant ata) {
        return new FlightEventJson(messageId, TYPE_PREFIX + name, VERSION, occurredOn, flightId.id(),
            flightNumber.value(), scheduledDepartureDate, departureAirport.value(),
            arrivalAirport == null ? null : arrivalAirport.value(),
            registration == null ? null : registration.value(), std, sta, etd, eta, atd, ata);
    }
}
