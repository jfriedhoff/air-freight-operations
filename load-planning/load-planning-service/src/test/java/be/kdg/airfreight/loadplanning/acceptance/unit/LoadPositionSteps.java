package be.kdg.airfreight.loadplanning.acceptance.unit;

import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadPosition;
import be.kdg.airfreight.loadplanning.domain.flightcargo.UldCode;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/** Placing a ULD on a load position and clearing it. */
public class LoadPositionSteps {
    private static final String NOT_ON_THE_FLOOR_PLAN = "is not on the floor plan";

    private final CargoWorld world;
    /** The ULD of the last When step. */
    private UldCode changed;

    public LoadPositionSteps(CargoWorld world) {
        this.world = world;
    }

    /** A Given before the When (puts the ULD there) and a Then after it (checks that it is there). */
    @Given("{uld} is on {word}")
    public void isOn(UldCode code, String position) {
        if (world.acted) {
            world.assertAccepted();
            assertThat(world.uld(code).getPosition()).extracting(LoadPosition::name).isEqualTo(position);
        } else {
            world.place(code, position);
        }
    }

    /** A name that is not on the floor plan becomes a position without a side, which the aggregate must refuse. */
    @When("I place {uld} on {word}")
    public void iPlace(UldCode code, String positionName) {
        Optional<LoadPosition> onTheFloorPlan = world.floorPlan.positionNamed(positionName);
        if (onTheFloorPlan.isEmpty()) {
            world.expectedRefusal = NOT_ON_THE_FLOOR_PLAN;
        }
        LoadPosition position = onTheFloorPlan.orElse(new LoadPosition(positionName, null));
        changed = code;
        world.attempt(() -> world.cargo.assign(code, position, world.planner, world.now, world.floorPlan,
            CargoWorld.POLICY));
    }

    @When("I clear the load position of {uld}")
    public void iClear(UldCode code) {
        changed = code;
        world.attempt(() -> world.cargo.clear(code, world.planner, world.now, CargoWorld.POLICY));
    }

    @Then("{word} is free")
    public void isFree(String position) {
        LoadPosition free = world.floorPlan.positionNamed(position).orElseThrow();
        assertThat(world.cargo.getUlds()).noneMatch(uld -> uld.isAt(free));
    }

    @Then("{uld} no longer has a load position")
    public void noLongerHasALoadPosition(UldCode code) {
        world.assertAccepted();
        assertThat(world.uld(code).hasPosition()).isFalse();
    }

    /** The state of the ULD; the event that the application service publishes is in FlightCargoTest. */
    @Then("it is recorded that I assigned the load position")
    @Then("it is recorded that I cleared the load position")
    public void itIsRecordedThatIChangedTheLoadPosition() {
        world.assertAccepted();
        assertThat(world.uld(changed).getPositionChangedBy()).isEqualTo(world.planner);
    }
}
