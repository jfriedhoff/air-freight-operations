package be.kdg.airfreight.flightoperations.domain.flight;

import java.time.Instant;
import java.time.LocalDate;

import be.kdg.airfreight.flightoperations.domain.flightplanner.FlightPlannerId;
import org.jmolecules.event.annotation.DomainEvent;

@DomainEvent
public record FlightLanded(FlightId flightId, FlightNumber flightNumber, LocalDate scheduledDepartureDate,
                           AirportCode departureAirport, AirportCode arrivalAirport,
                           AircraftRegistration registration, Instant ata, FlightPlannerId registeredBy,
                           Instant occurredOn) implements FlightEvent {
}
