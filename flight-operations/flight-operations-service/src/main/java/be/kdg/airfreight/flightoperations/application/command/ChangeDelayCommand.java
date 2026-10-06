package be.kdg.airfreight.flightoperations.application.command;

import be.kdg.airfreight.flightoperations.domain.flight.DelayDuration;
import be.kdg.airfreight.flightoperations.domain.flight.DelayId;
import be.kdg.airfreight.flightoperations.domain.flight.FlightId;
import be.kdg.airfreight.flightoperations.domain.flightplanner.FlightPlannerId;

public record ChangeDelayCommand(FlightId flightId, DelayId delayId, DelayDuration minutes, String description,
                                 FlightPlannerId by) {
}
