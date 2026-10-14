package be.kdg.airfreight.loadplanning.infrastructure.loadplanner;

import java.util.List;

import be.kdg.airfreight.loadplanning.domain.loadplanner.LoadPlanner;
import be.kdg.airfreight.loadplanning.domain.loadplanner.LoadPlannerId;
import be.kdg.airfreight.loadplanning.domain.loadplanner.LoadPlannerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/** Registers the configured load planners at startup, if they are not there yet. */
@Slf4j
@Component
@EnableConfigurationProperties(SeedProperties.class)
class LoadPlannerSeeder implements ApplicationRunner {
    private final SeedProperties seed;
    private final LoadPlannerRepository loadPlanners;

    LoadPlannerSeeder(SeedProperties seed, LoadPlannerRepository loadPlanners) {
        this.seed = seed;
        this.loadPlanners = loadPlanners;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<SeedProperties.Planner> planners = seed.planners() == null ? List.of() : seed.planners();
        for (SeedProperties.Planner planner : planners) {
            LoadPlannerId id = new LoadPlannerId(planner.id());
            if (loadPlanners.findById(id).isEmpty()) {
                loadPlanners.save(new LoadPlanner(id, planner.name()));
                log.info("Load planner {} ({}) registered", planner.name(), planner.id());
            }
        }
    }
}
