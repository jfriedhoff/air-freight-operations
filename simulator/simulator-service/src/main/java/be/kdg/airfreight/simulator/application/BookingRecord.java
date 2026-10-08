package be.kdg.airfreight.simulator.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import be.kdg.airfreight.simulator.infrastructure.messaging.UldMessage;

/** One booking record as the trainer supplies it: the flight is named by its full scheduled departure date. */
public record BookingRecord(String flightNumber, LocalDate scheduledDepartureDate, String departureAirport,
                            String uldCode, BigDecimal weight, String weightUnit, String destination) {
    /** The real booking system only sends the day of the month; Messaging derives the date again. */
    public UldMessage toUldMessage(UUID messageId, Instant createdAt) {
        return new UldMessage(messageId, UldMessage.TYPE, UldMessage.VERSION, createdAt, flightNumber,
            scheduledDepartureDate.getDayOfMonth(), departureAirport, uldCode, weight, weightUnit, destination);
    }
}
