package be.kdg.airfreight.loadplanning.application.command;

import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargoId;
import be.kdg.airfreight.loadplanning.domain.flightcargo.UldCode;
import be.kdg.airfreight.loadplanning.domain.loadplanner.LoadPlannerId;

/** The position is given by its name; the floor plan of the flight's aircraft type knows the rest. */
public record AssignLoadPositionCommand(FlightCargoId flightCargoId, UldCode uldCode, String positionName,
                                        LoadPlannerId by) {
}
