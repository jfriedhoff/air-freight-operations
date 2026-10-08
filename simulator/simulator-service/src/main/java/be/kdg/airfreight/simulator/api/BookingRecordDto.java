package be.kdg.airfreight.simulator.api;

import java.math.BigDecimal;
import java.time.LocalDate;

import be.kdg.airfreight.simulator.application.BookingRecord;
import jakarta.validation.constraints.NotNull;

/**
 * One booking record in our own JSON: the flight by flight number, scheduled departure date and
 * departure airport. Only "is it there" is checked; Messaging and Load Planning judge the values.
 */
public record BookingRecordDto(
    @NotNull(message = "flightNumber is required") String flightNumber,
    @NotNull(message = "scheduledDepartureDate is required") LocalDate scheduledDepartureDate,
    @NotNull(message = "departureAirport is required") String departureAirport,
    @NotNull(message = "uldCode is required") String uldCode,
    @NotNull(message = "weight is required") BigDecimal weight,
    @NotNull(message = "weightUnit is required") String weightUnit,
    @NotNull(message = "destination is required") String destination) {

    BookingRecord toRecord() {
        return new BookingRecord(flightNumber, scheduledDepartureDate, departureAirport, uldCode, weight,
            weightUnit, destination);
    }
}
