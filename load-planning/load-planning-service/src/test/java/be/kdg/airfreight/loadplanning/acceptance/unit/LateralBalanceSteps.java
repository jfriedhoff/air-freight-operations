package be.kdg.airfreight.loadplanning.acceptance.unit;

import be.kdg.airfreight.loadplanning.domain.flightcargo.check.LateralBalanceCheck;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** The lateral balance check. */
public class LateralBalanceSteps {
    private static final String HARD_CHECK_FAILS = "a hard load check fails";

    private final CargoWorld world;

    public LateralBalanceSteps(CargoWorld world) {
        this.world = world;
    }

    /** Right carries more than 110 % of left; finalizing must then be refused for that reason. */
    @Given("the lateral balance fails")
    public void theLateralBalanceFails() {
        world.loadWeightOn("CL", "1000 kg");
        world.loadWeightOn("CR", "1200 kg");
        assertThat(world.cargo.checkLoad(List.of(new LateralBalanceCheck(CargoWorld.LATERAL_BALANCE_LIMIT)),
                world.floorPlan, world.now, CargoWorld.POLICY).hardChecksPassed())
            .as("the lateral balance passes").isFalse();
        world.expectedRefusal = HARD_CHECK_FAILS;
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
