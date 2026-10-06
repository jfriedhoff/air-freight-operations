package be.kdg.airfreight.flightoperations.domain.flight;

import java.time.Instant;
import java.time.LocalDate;

import org.jmolecules.event.annotation.DomainEvent;

@DomainEvent
public record FinalizedLoadReceived(FlightId flightId, FlightNumber flightNumber, LocalDate scheduledDepartureDate,
                                    AirportCode departureAirport, Instant occurredOn) implements FlightEvent {
}
