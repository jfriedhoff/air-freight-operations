package be.kdg.airfreight.loadplanning.infrastructure.loadplanner;

import java.util.Optional;

import be.kdg.airfreight.loadplanning.domain.loadplanner.LoadPlanner;
import be.kdg.airfreight.loadplanning.domain.loadplanner.LoadPlannerId;
import be.kdg.airfreight.loadplanning.domain.loadplanner.LoadPlannerRepository;
import be.kdg.airfreight.loadplanning.infrastructure.loadplanner.jpa.JpaLoadPlanner;
import be.kdg.airfreight.loadplanning.infrastructure.loadplanner.jpa.JpaLoadPlannerRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("db")
class DbLoadPlannerRepository implements LoadPlannerRepository {
    private final JpaLoadPlannerRepository jpaLoadPlanners;

    DbLoadPlannerRepository(JpaLoadPlannerRepository jpaLoadPlanners) {
        this.jpaLoadPlanners = jpaLoadPlanners;
    }

    @Override
    public Optional<LoadPlanner> findById(LoadPlannerId id) {
        return jpaLoadPlanners.findById(id.id()).map(JpaLoadPlanner::toDomain);
    }

    @Override
    public void save(LoadPlanner loadPlanner) {
        jpaLoadPlanners.save(JpaLoadPlanner.fromDomain(loadPlanner));
    }
}
