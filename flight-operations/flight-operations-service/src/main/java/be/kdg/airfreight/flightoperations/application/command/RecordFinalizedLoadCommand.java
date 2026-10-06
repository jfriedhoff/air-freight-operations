package be.kdg.airfreight.flightoperations.application.command;

import java.time.LocalDate;

import be.kdg.airfreight.flightoperations.domain.flight.AirportCode;
import be.kdg.airfreight.flightoperations.domain.flight.FlightNumber;

public record RecordFinalizedLoadCommand(FlightNumber flightNumber, LocalDate scheduledDepartureDate,
                                         AirportCode departureAirport) {
}
