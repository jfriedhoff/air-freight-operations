package be.kdg.airfreight.flightoperations.infrastructure.flightplanner;

import java.util.List;

import be.kdg.airfreight.flightoperations.domain.flightplanner.FlightPlanner;
import be.kdg.airfreight.flightoperations.domain.flightplanner.FlightPlannerId;
import be.kdg.airfreight.flightoperations.domain.flightplanner.FlightPlannerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/** Registers the configured flight planners at startup, if they are not there yet. */
@Slf4j
@Component
@EnableConfigurationProperties(SeedProperties.class)
class FlightPlannerSeeder implements ApplicationRunner {
    private final SeedProperties seed;
    private final FlightPlannerRepository flightPlanners;

    FlightPlannerSeeder(SeedProperties seed, FlightPlannerRepository flightPlanners) {
        this.seed = seed;
        this.flightPlanners = flightPlanners;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<SeedProperties.Planner> planners = seed.planners() == null ? List.of() : seed.planners();
        for (SeedProperties.Planner planner : planners) {
            FlightPlannerId id = new FlightPlannerId(planner.id());
            if (flightPlanners.findById(id).isEmpty()) {
                flightPlanners.save(new FlightPlanner(id, planner.name()));
                log.info("Flight planner {} ({}) registered", planner.name(), planner.id());
            }
        }
    }
}
