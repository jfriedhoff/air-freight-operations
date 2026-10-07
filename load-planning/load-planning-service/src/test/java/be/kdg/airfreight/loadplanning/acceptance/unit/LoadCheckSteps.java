package be.kdg.airfreight.loadplanning.acceptance.unit;

import be.kdg.airfreight.loadplanning.domain.flightcargo.CheckSeverity;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

public class LoadCheckSteps {
    private final CargoWorld world;

    public LoadCheckSteps(CargoWorld world) {
        this.world = world;
    }

    @When("the load is checked")
    public void theLoadIsChecked() {
        world.checkAll();
    }

    @Then("the check has passed")
    public void theCheckHasPassed() {
        assertThat(world.result.passed()).withFailMessage("The check failed: %s", world.result.reason()).isTrue();
    }

    @Then("the check has failed")
    public void theCheckHasFailed() {
        assertThat(world.result.passed()).isFalse();
    }

    @Then("the soft check {string} has passed")
    public void theSoftCheckHasPassed(String name) {
        selectSoftCheck(name);
        theCheckHasPassed();
    }

    @Then("the soft check {string} has failed")
    public void theSoftCheckHasFailed(String name) {
        selectSoftCheck(name);
        theCheckHasFailed();
    }

    /** Takes the result of one check from the report, so the next steps read that one. */
    private void selectSoftCheck(String name) {
        world.result = world.report.results().stream()
            .filter(result -> result.checkName().equalsIgnoreCase(name))
            .findFirst()
            .orElseThrow(() -> new AssertionError("The report has no check \"" + name + "\""));
        assertThat(world.result.severity()).isEqualTo(CheckSeverity.SOFT);
    }
}
