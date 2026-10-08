package be.kdg.airfreight.loadplanning.acceptance.unit;

import be.kdg.airfreight.loadplanning.domain.flightcargo.UldCode;
import io.cucumber.java.en.Then;

import static org.assertj.core.api.Assertions.assertThat;

/** The soft check that all booked cargo has a load position. It only runs with all checks together. */
public class AllCargoLoadedSteps {
    private final CargoWorld world;

    public AllCargoLoadedSteps(CargoWorld world) {
        this.world = world;
    }

    @Then("the report states that {uld} has no load position")
    public void theReportStatesThatItHasNoLoadPosition(UldCode code) {
        assertThat(world.checkResult.reason()).isEqualTo(code.value() + " has no load position");
    }
}
