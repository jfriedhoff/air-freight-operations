package be.kdg.airfreight.loadplanning.application;

import java.time.LocalDate;
import java.util.List;

import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargo;

/** The cargos of the flights of one day, with the day they are for: the caller may not have named one. */
public record FlightsToLoad(LocalDate scheduledDepartureDate, List<FlightCargo> flights) {
}
