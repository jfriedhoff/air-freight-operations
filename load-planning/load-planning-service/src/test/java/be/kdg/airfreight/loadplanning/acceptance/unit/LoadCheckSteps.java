package be.kdg.airfreight.loadplanning.acceptance.unit;

import io.cucumber.java.en.Then;

import static org.assertj.core.api.Assertions.assertThat;

public class LoadCheckSteps {
    private final CargoWorld world;

    public LoadCheckSteps(CargoWorld world) {
        this.world = world;
    }

    @Then("the check has passed")
    public void theCheckHasPassed() {
        assertThat(world.result.passed()).withFailMessage("The check failed: %s", world.result.reason()).isTrue();
    }

    @Then("the check has failed")
    public void theCheckHasFailed() {
        assertThat(world.result.passed()).isFalse();
    }
}
