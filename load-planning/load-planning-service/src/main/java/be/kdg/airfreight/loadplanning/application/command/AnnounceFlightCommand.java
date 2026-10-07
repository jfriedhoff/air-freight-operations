package be.kdg.airfreight.loadplanning.application.command;

import java.time.Instant;

import be.kdg.airfreight.loadplanning.domain.flightcargo.AirportCode;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightId;

public record AnnounceFlightCommand(FlightId flightId, AirportCode arrivalAirport, Instant std, Instant sta) {
}
