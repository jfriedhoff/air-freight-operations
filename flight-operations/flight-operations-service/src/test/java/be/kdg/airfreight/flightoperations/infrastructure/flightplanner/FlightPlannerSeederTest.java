package be.kdg.airfreight.flightoperations.infrastructure.flightplanner;

import be.kdg.airfreight.flightoperations.domain.flightplanner.FlightPlanner;
import be.kdg.airfreight.flightoperations.domain.flightplanner.FlightPlannerId;
import be.kdg.airfreight.flightoperations.domain.flightplanner.FlightPlannerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class FlightPlannerSeederTest {
    private static final FlightPlannerId SEEDED =
        new FlightPlannerId(UUID.fromString("3f6b1c1e-5a0d-4d7e-9c1a-2b7f4e8a1001"));

    @Autowired
    private FlightPlannerRepository flightPlanners;

    @Test
    void theConfiguredFlightPlannersExistAfterStartup() {
        assertThat(flightPlanners.findById(SEEDED)).map(FlightPlanner::getName).contains("Fien Flightplanner");
    }
}
