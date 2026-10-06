package be.kdg.airfreight.messaging.domain.booking;

import java.math.BigDecimal;
import java.time.Instant;

import be.kdg.airfreight.messaging.domain.AirportCode;
import be.kdg.airfreight.messaging.domain.FlightId;
import org.jmolecules.event.annotation.DomainEvent;

/** One ULD booked on a flight, with the flight's full scheduled departure date. */
@DomainEvent
public record UldBookingReceived(FlightId flightId, String uldCode, BigDecimal weight, String weightUnit,
                                 AirportCode destination, Instant occurredOn) {
}
