package be.kdg.airfreight.loadplanning.application.command;

import be.kdg.airfreight.loadplanning.domain.flightcargo.AirportCode;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightId;
import be.kdg.airfreight.loadplanning.domain.flightcargo.UldCode;
import be.kdg.airfreight.loadplanning.domain.flightcargo.Weight;

public record BookUldCommand(FlightId flightId, UldCode uldCode, Weight weight, AirportCode destination) {
}
