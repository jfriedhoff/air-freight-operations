package be.kdg.airfreight.loadplanning.application;

import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargo;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FloorPlan;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadingPeriod;

/**
 * The cargo of a flight at one moment: the cargo, the floor plan of its aircraft type, its loading period and
 * whether the load can change right now. What a use case hands back when the answer depends on the time: the
 * aggregate answers the last two, the use case only gives it the time and the policy.
 */
public record FlightCargoSnapshot(FlightCargo cargo, FloorPlan floorPlan, LoadingPeriod loadingPeriod,
                                  boolean loadCanChange) {
}
