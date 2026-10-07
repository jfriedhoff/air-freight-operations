package be.kdg.airfreight.loadplanning.domain.loadplanner;

import java.util.Optional;

public interface LoadPlannerRepository {
    Optional<LoadPlanner> findById(LoadPlannerId id);

    void save(LoadPlanner loadPlanner);
}
