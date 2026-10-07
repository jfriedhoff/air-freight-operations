package be.kdg.airfreight.flightoperations.domain.flight;

import java.time.Instant;
import java.time.LocalDate;

import org.jmolecules.event.annotation.DomainEvent;

/** The flight was deleted after its retention period, whatever its state. */
@DomainEvent
public record FlightRemoved(FlightId flightId, FlightNumber flightNumber, LocalDate scheduledDepartureDate,
                            AirportCode departureAirport, Instant occurredOn) implements FlightEvent {
}
