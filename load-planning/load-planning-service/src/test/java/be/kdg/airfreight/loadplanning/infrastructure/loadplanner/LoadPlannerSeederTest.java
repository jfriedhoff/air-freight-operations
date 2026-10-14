package be.kdg.airfreight.loadplanning.infrastructure.loadplanner;

import java.util.UUID;

import be.kdg.airfreight.loadplanning.TestClock;
import be.kdg.airfreight.loadplanning.domain.loadplanner.LoadPlanner;
import be.kdg.airfreight.loadplanning.domain.loadplanner.LoadPlannerId;
import be.kdg.airfreight.loadplanning.domain.loadplanner.LoadPlannerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestClock.class)
class LoadPlannerSeederTest {
    private static final LoadPlannerId SEEDED =
        new LoadPlannerId(UUID.fromString("3f6b1c1e-5a0d-4d7e-9c1a-2b7f4e8a1002"));

    @Autowired
    private LoadPlannerRepository loadPlanners;

    @Test
    void theConfiguredLoadPlannersExistAfterStartup() {
        assertThat(loadPlanners.findById(SEEDED)).map(LoadPlanner::getName).contains("Lars Loadplanner");
    }
}
