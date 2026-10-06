package be.kdg.airfreight.flightoperations.application.command;

import be.kdg.airfreight.flightoperations.domain.flight.AircraftRegistration;
import be.kdg.airfreight.flightoperations.domain.flight.AirportCode;
import be.kdg.airfreight.flightoperations.domain.flight.FlightNumber;
import be.kdg.airfreight.flightoperations.domain.flight.ScheduledTimes;

public record ScheduleFlightCommand(FlightNumber flightNumber, AirportCode departureAirport,
                                    AirportCode arrivalAirport, AircraftRegistration registration,
                                    ScheduledTimes scheduled) {
}
