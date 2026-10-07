package be.kdg.airfreight.flightoperations.application.command;

import java.time.Instant;

import be.kdg.airfreight.flightoperations.domain.flight.FlightId;
import be.kdg.airfreight.flightoperations.domain.flightplanner.FlightPlannerId;

public record RegisterDepartureCommand(FlightId flightId, Instant atd, FlightPlannerId by) {
}
