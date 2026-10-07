package be.kdg.airfreight.flightoperations.application.command;

import be.kdg.airfreight.flightoperations.domain.flight.DelayCode;
import be.kdg.airfreight.flightoperations.domain.flight.DelayDuration;
import be.kdg.airfreight.flightoperations.domain.flight.DelayKind;
import be.kdg.airfreight.flightoperations.domain.flight.FlightId;
import be.kdg.airfreight.flightoperations.domain.flightplanner.FlightPlannerId;

public record AddDelayCommand(FlightId flightId, DelayKind kind, DelayDuration minutes, DelayCode reason,
                              String description, FlightPlannerId by) {
}
