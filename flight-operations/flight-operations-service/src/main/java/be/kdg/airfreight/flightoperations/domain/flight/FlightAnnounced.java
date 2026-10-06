package be.kdg.airfreight.flightoperations.domain.flight;

import java.time.Instant;
import java.time.LocalDate;

import org.jmolecules.event.annotation.DomainEvent;

@DomainEvent
public record FlightAnnounced(FlightId flightId, FlightNumber flightNumber, LocalDate scheduledDepartureDate,
                              AirportCode departureAirport, AirportCode arrivalAirport,
                              AircraftRegistration registration, Instant std, Instant sta,
                              Instant occurredOn) implements FlightEvent {
}
