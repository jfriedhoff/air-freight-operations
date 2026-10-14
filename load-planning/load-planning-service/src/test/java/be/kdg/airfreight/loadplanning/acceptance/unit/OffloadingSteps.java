package be.kdg.airfreight.loadplanning.acceptance.unit;

import be.kdg.airfreight.loadplanning.domain.flightcargo.UldCode;
import be.kdg.airfreight.loadplanning.domain.flightcargo.check.OffloadingCheck;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

/** The offloading check. */
public class OffloadingSteps {
    private final CargoWorld world;

    public OffloadingSteps(CargoWorld world) {
        this.world = world;
    }

    @When("the offloading is checked")
    public void theOffloadingIsChecked() {
        world.check(new OffloadingCheck());
    }

    @Then("the report states that {uld} on {word} cannot be offloaded because {uld} on {word} is in the way")
    public void theReportStates(UldCode stuck, String stuckPosition, UldCode blocking, String blockingPosition) {
        assertThat(world.checkResult.reason()).isEqualTo(stuck.value() + " on " + stuckPosition
            + " cannot be offloaded because " + blocking.value() + " on " + blockingPosition + " is in the way");
    }

    /** The whole report as a doc string, for a report that names several ULDs. */
    @Then("the report states:")
    public void theReportStatesExactly(String report) {
        assertThat(world.checkResult.reason()).isEqualTo(report.strip());
    }
}
