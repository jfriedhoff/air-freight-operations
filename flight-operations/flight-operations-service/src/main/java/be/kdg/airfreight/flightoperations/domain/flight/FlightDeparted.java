package be.kdg.airfreight.flightoperations.domain.flight;

import java.time.Instant;
import java.time.LocalDate;

import be.kdg.airfreight.flightoperations.domain.flightplanner.FlightPlannerId;
import org.jmolecules.event.annotation.DomainEvent;

@DomainEvent
public record FlightDeparted(FlightId flightId, FlightNumber flightNumber, LocalDate scheduledDepartureDate,
                             AirportCode departureAirport, AircraftRegistration registration,
                             Instant atd, FlightPlannerId registeredBy,
                             Instant occurredOn) implements FlightEvent {
}
