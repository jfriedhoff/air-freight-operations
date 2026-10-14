package be.kdg.airfreight.loadplanning.api.flightcargo;

import java.time.LocalDate;
import java.util.List;

import be.kdg.airfreight.loadplanning.application.FlightsToLoad;

/** The flights of one day. The day is in the answer: without a date in the request the server takes today. */
public record FlightsToLoadDto(LocalDate scheduledDepartureDate, List<FlightToLoadDto> flights) {
    public static FlightsToLoadDto from(FlightsToLoad flightsToLoad) {
        return new FlightsToLoadDto(flightsToLoad.scheduledDepartureDate(),
            flightsToLoad.flights().stream().map(FlightToLoadDto::from).toList());
    }
}
