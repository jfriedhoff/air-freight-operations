package be.kdg.airfreight.simulator.application;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One booking record as the trainer supplies it: the flight is named by its full scheduled departure date. */
public record BookingRecord(String flightNumber, LocalDate scheduledDepartureDate, String departureAirport,
                            String uldCode, BigDecimal weight, String weightUnit, String destination) {
}
