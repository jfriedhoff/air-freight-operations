package be.kdg.airfreight.loadplanning.acceptance.unit;

import be.kdg.airfreight.loadplanning.domain.flightcargo.UldCode;
import io.cucumber.java.en.But;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Finalizing the load. */
public class FinalizeLoadSteps {
    private static final String FINALIZED = "finalized and can no longer change";

    private final CargoWorld world;

    public FinalizeLoadSteps(CargoWorld world) {
        this.world = world;
    }

    /** Without load checks: they are not what these scenarios test, and a ULD on CL alone fails the lateral balance. */
    @Given("the load has been finalized")
    public void theLoadHasBeenFinalized() {
        world.finalizeLoad(List.of());
        world.expectedRefusal = FINALIZED;
    }

    @When("I click Finalize Load")
    public void iClickFinalizeLoad() {
        world.attempt(() -> world.finalizeLoad(CargoWorld.CHECKS));
    }

    @Then("the load is finalized")
    @Then("the load remains finalized")
    public void theLoadIsFinalized() {
        world.assertAccepted();
        assertThat(world.cargo.isFinalized()).isTrue();
    }

    @Then("the load is not finalized")
    public void theLoadIsNotFinalized() {
        world.assertRefusedForTheExpectedReason();
        assertThat(world.cargo.isFinalized()).isFalse();
    }

    /** Tries to put a newly booked ULD on a free position: that must be refused because the load is finalized. */
    @Then("I can no longer change load positions")
    public void iCanNoLongerChangeLoadPositions() {
        UldCode code = world.nextCode();
        world.book(code, CargoWorld.ANY_WEIGHT);
        world.expectedRefusal = FINALIZED;
        world.attempt(() -> world.place(code, world.freePosition()));
        world.assertRefusedForTheExpectedReason();
    }

    /** The state of the cargo; the event that becomes the CPM is in FlightCargoTest. */
    @Then("it is recorded that I finalized the load")
    public void itIsRecordedThatIFinalizedTheLoad() {
        assertThat(world.cargo.getFinalizedBy()).isEqualTo(world.planner);
    }

    @But("the load can still be finalized if all hard checks pass")
    public void theLoadCanStillBeFinalized() {
        assertThat(world.report.hardChecksPassed()).isTrue();
        world.finalizeLoad(CargoWorld.CHECKS);
        assertThat(world.cargo.isFinalized()).isTrue();
    }
}
