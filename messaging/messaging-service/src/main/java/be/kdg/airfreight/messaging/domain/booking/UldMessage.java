package be.kdg.airfreight.messaging.domain.booking;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

import be.kdg.airfreight.messaging.domain.AirportCode;
import be.kdg.airfreight.messaging.domain.DomainRuleViolationException;
import be.kdg.airfreight.messaging.domain.FlightId;
import be.kdg.airfreight.messaging.domain.FlightNumber;
import be.kdg.airfreight.messaging.domain.FormatVersion;
import be.kdg.airfreight.messaging.domain.MessageId;
import be.kdg.airfreight.messaging.domain.receivedmessage.IncomingMessage;
import org.jmolecules.ddd.annotation.ValueObject;

/** A ULD message as read. It names the flight by the day of the month only, as a telegram does. */
@ValueObject
public record UldMessage(MessageId id, FormatVersion version, FlightNumber flightNumber, int dayOfMonth,
                         AirportCode departureAirport, UldCode uldCode, BigDecimal weight, WeightUnit weightUnit,
                         AirportCode destination) implements IncomingMessage {
    public UldMessage {
        Objects.requireNonNull(id, "Id cannot be null");
        Objects.requireNonNull(version, "Version cannot be null");
        Objects.requireNonNull(flightNumber, "Flight number cannot be null");
        Objects.requireNonNull(departureAirport, "Departure airport cannot be null");
        Objects.requireNonNull(uldCode, "ULD code cannot be null");
        Objects.requireNonNull(weight, "Weight cannot be null");
        Objects.requireNonNull(weightUnit, "Weight unit cannot be null");
        Objects.requireNonNull(destination, "Destination cannot be null");
        if (weight.signum() <= 0) {
            throw new DomainRuleViolationException("The weight of a ULD must be greater than 0");
        }
    }

    public UldBookingReceived bookingOn(LocalDate scheduledDepartureDate, Instant now) {
        return new UldBookingReceived(new FlightId(flightNumber, scheduledDepartureDate, departureAirport),
            uldCode.value(), weight, weightUnit.name(), destination, now);
    }
}
