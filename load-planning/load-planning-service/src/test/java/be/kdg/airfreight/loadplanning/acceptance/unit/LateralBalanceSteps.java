package be.kdg.airfreight.loadplanning.acceptance.unit;

import be.kdg.airfreight.loadplanning.domain.flightcargo.UldCode;
import be.kdg.airfreight.loadplanning.domain.flightcargo.check.LateralBalanceCheck;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

public class LateralBalanceSteps {
    private final CargoWorld world;

    public LateralBalanceSteps(CargoWorld world) {
        this.world = world;
    }

    @Given("the L positions hold {weight} in total and the R positions {weight}")
    public void theLAndRPositionsHold(String left, String right) {
        world.load("CL", left);
        world.load("CR", right);
    }

    @Given("the left side holds {weight} and the right side {weight}")
    public void theLeftAndRightSideHold(String left, String right) {
        theLAndRPositionsHold(left, right);
    }

    @Given("{word} holds {weight} and {word} {weight}")
    public void twoPositionsHold(String first, String firstWeight, String second, String secondWeight) {
        world.load(first, firstWeight);
        world.load(second, secondWeight);
    }

    @Given("{word} holds {weight}, {word} {weight} and {word} {weight}")
    public void threePositionsHold(String first, String firstWeight, String second, String secondWeight,
                                   String third, String thirdWeight) {
        world.load(first, firstWeight);
        twoPositionsHold(second, secondWeight, third, thirdWeight);
    }

    @Given("{word} of {weight} is booked without a load position")
    public void isBookedWithoutALoadPosition(String code, String weight) {
        world.book(new UldCode(code), weight);
    }

    @When("the lateral balance is checked")
    public void theLateralBalanceIsChecked() {
        world.check(new LateralBalanceCheck(CargoWorld.LATERAL_BALANCE_LIMIT));
    }

    @Then("the report states {weight} left, {weight} right and a maximum of {weight} on the heaviest side")
    public void theReportStates(String left, String right, String maximum) {
        assertThat(world.result.reason()).isEqualTo(
            "Left carries " + left + " and right " + right + "; the heaviest side may carry at most " + maximum);
    }

    @Then("the report states {weight} left, {weight} right and a maximum of {weight}, all in kg")
    public void theReportStatesInKg(String left, String right, String maximum) {
        theReportStates(left, right, maximum);
    }
}
