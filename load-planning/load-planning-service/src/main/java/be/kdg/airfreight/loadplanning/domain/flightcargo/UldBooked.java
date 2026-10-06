package be.kdg.airfreight.loadplanning.domain.flightcargo;

import java.time.Instant;

import org.jmolecules.event.annotation.DomainEvent;

@DomainEvent
public record UldBooked(FlightCargoId flightCargoId, FlightId flightId, UldCode uldCode, Weight weight,
                        AirportCode destination, Instant occurredOn) implements FlightCargoEvent {
}
