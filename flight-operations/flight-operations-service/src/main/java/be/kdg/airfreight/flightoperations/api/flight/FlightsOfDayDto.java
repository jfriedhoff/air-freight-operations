package be.kdg.airfreight.flightoperations.api.flight;

import java.time.LocalDate;
import java.util.List;

import be.kdg.airfreight.flightoperations.application.FlightsOfDay;

/** The flights of one day. The day is in the answer: without a date in the request the server takes today (UTC). */
public record FlightsOfDayDto(LocalDate scheduledDepartureDate, List<FlightRowDto> flights) {
    public static FlightsOfDayDto from(FlightsOfDay flightsOfDay) {
        return new FlightsOfDayDto(flightsOfDay.scheduledDepartureDate(),
            flightsOfDay.flights().stream().map(FlightRowDto::from).toList());
    }
}
