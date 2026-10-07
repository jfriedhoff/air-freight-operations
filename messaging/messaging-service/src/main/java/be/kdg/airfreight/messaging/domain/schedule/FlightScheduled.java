package be.kdg.airfreight.messaging.domain.schedule;

import java.time.Instant;

import be.kdg.airfreight.messaging.domain.AircraftRegistration;
import be.kdg.airfreight.messaging.domain.AirportCode;
import be.kdg.airfreight.messaging.domain.FlightNumber;
import org.jmolecules.event.annotation.DomainEvent;

/** One flight from an SSM, with STD and STA as full UTC moments. */
@DomainEvent
public record FlightScheduled(FlightNumber flightNumber, AirportCode departureAirport, AirportCode arrivalAirport,
                              AircraftRegistration registration, Instant std, Instant sta, Instant occurredOn) {
}
