package be.kdg.airfreight.loadplanning.acceptance.unit;

import be.kdg.airfreight.loadplanning.domain.flightcargo.UldCode;
import io.cucumber.java.en.But;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;

import static org.assertj.core.api.Assertions.assertThat;

public class CargoWithoutPositionSteps {
    private static final String ANY_WEIGHT = "1000 kg";

    private final CargoWorld world;

    public CargoWithoutPositionSteps(CargoWorld world) {
        this.world = world;
    }

    @Given("{word} is booked but has no load position")
    public void isBookedButHasNoLoadPosition(String code) {
        world.book(new UldCode(code), ANY_WEIGHT);
    }

    @Given("{word} and {word} are booked and both have a load position")
    public void areBookedAndBothHaveALoadPosition(String first, String second) {
        world.book(new UldCode(first), ANY_WEIGHT);
        world.place(new UldCode(first), "CL");
        world.book(new UldCode(second), ANY_WEIGHT);
        world.place(new UldCode(second), "CR");
    }

    @Given("no cargo is booked on the flight")
    public void noCargoIsBooked() {
        // the flight starts without cargo
    }

    @Then("the report states that {word} has no load position")
    public void theReportStatesThatItHasNoLoadPosition(String code) {
        assertThat(world.result.reason()).isEqualTo(code + " has no load position");
    }

    @But("the load can still be finalized if all hard checks pass")
    public void theLoadCanStillBeFinalized() {
        assertThat(world.report.hardChecksPassed()).isTrue();
        world.cargo.finalizeLoad(world.planner, CargoWorld.NOW, CargoWorld.CHECKS, world.floorPlan, CargoWorld.POLICY);
        assertThat(world.cargo.isFinalized()).isTrue();
    }
}
