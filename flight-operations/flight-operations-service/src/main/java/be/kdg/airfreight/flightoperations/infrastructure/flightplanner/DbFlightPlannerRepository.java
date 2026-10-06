package be.kdg.airfreight.flightoperations.infrastructure.flightplanner;

import java.util.Optional;

import be.kdg.airfreight.flightoperations.domain.flightplanner.FlightPlanner;
import be.kdg.airfreight.flightoperations.domain.flightplanner.FlightPlannerId;
import be.kdg.airfreight.flightoperations.domain.flightplanner.FlightPlannerRepository;
import be.kdg.airfreight.flightoperations.infrastructure.flightplanner.jpa.JpaFlightPlanner;
import be.kdg.airfreight.flightoperations.infrastructure.flightplanner.jpa.JpaFlightPlannerRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("db")
class DbFlightPlannerRepository implements FlightPlannerRepository {
    private final JpaFlightPlannerRepository jpaFlightPlanners;

    DbFlightPlannerRepository(JpaFlightPlannerRepository jpaFlightPlanners) {
        this.jpaFlightPlanners = jpaFlightPlanners;
    }

    @Override
    public Optional<FlightPlanner> findById(FlightPlannerId id) {
        return jpaFlightPlanners.findById(id.id()).map(JpaFlightPlanner::toDomain);
    }

    @Override
    public void save(FlightPlanner flightPlanner) {
        jpaFlightPlanners.save(JpaFlightPlanner.fromDomain(flightPlanner));
    }
}
