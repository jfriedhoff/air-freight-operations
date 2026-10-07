package be.kdg.airfreight.loadplanning.application.command;

import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargoId;
import be.kdg.airfreight.loadplanning.domain.flightcargo.UldCode;
import be.kdg.airfreight.loadplanning.domain.loadplanner.LoadPlannerId;

public record ClearLoadPositionCommand(FlightCargoId flightCargoId, UldCode uldCode, LoadPlannerId by) {
}
