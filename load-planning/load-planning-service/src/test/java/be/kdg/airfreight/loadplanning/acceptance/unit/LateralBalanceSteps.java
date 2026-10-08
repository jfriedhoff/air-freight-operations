package be.kdg.airfreight.loadplanning.acceptance.unit;

import be.kdg.airfreight.loadplanning.domain.flightcargo.check.LateralBalanceCheck;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

/** The lateral balance check. */
public class LateralBalanceSteps {
    private final CargoWorld world;

    public LateralBalanceSteps(CargoWorld world) {
        this.world = world;
    }

    @When("the lateral balance is checked")
    public void theLateralBalanceIsChecked() {
        world.check(new LateralBalanceCheck(CargoWorld.LATERAL_BALANCE_LIMIT));
    }

    @Then("the report states {weight} left, {weight} right and a maximum of {weight} on the heaviest side")
    @Then("the report states {weight} left, {weight} right and a maximum of {weight}, all in kg")
    public void theReportStates(String left, String right, String maximum) {
        assertThat(world.checkResult.reason()).isEqualTo(
            "Left carries " + left + " and right " + right + "; the heaviest side may carry at most " + maximum);
    }
}
