package be.kdg.airfreight.loadplanning.application.command;

import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargoId;
import be.kdg.airfreight.loadplanning.domain.loadplanner.LoadPlannerId;

public record FinalizeLoadCommand(FlightCargoId flightCargoId, LoadPlannerId by) {
}
