package be.kdg.airfreight.flightoperations.domain.flight;

import java.time.Instant;
import java.time.LocalDate;

import be.kdg.airfreight.flightoperations.domain.flightplanner.FlightPlannerId;
import org.jmolecules.event.annotation.DomainEvent;

@DomainEvent
public record ArrivalEstimateChanged(FlightId flightId, FlightNumber flightNumber, LocalDate scheduledDepartureDate,
                                     AirportCode departureAirport, AirportCode arrivalAirport,
                                     AircraftRegistration registration, Instant eta, FlightPlannerId changedBy,
                                     Instant occurredOn) implements FlightEvent {
}
