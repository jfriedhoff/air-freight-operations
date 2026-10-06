package be.kdg.airfreight.flightoperations.domain.flightplanner;

import java.util.Optional;

public interface FlightPlannerRepository {
    Optional<FlightPlanner> findById(FlightPlannerId id);

    void save(FlightPlanner flightPlanner);
}
