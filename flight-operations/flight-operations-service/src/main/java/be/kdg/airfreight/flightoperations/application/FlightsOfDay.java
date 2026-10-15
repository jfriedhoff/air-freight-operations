package be.kdg.airfreight.flightoperations.application;

import java.time.LocalDate;
import java.util.List;

import be.kdg.airfreight.flightoperations.domain.flight.Flight;

/** The flights of one day, with the day they are for: the caller may not have named one. */
public record FlightsOfDay(LocalDate scheduledDepartureDate, List<Flight> flights) {
}
