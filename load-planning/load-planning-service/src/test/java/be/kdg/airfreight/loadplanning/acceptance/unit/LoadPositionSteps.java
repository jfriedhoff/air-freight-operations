package be.kdg.airfreight.loadplanning.acceptance.unit;

import be.kdg.airfreight.loadplanning.domain.DomainRuleViolationException;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadPosition;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadPositionAssigned;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadPositionCleared;
import be.kdg.airfreight.loadplanning.domain.flightcargo.UldCode;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Story 18. */
public class LoadPositionSteps {
    private static final String ANY_WEIGHT = "1000 kg";

    private final CargoWorld world;
    private LoadPositionAssigned assigned;
    private LoadPositionCleared cleared;

    public LoadPositionSteps(CargoWorld world) {
        this.world = world;
    }

    @Given("flight {word} with STD {moment} UTC")
    public void flightWithStd(String flightNumber, Instant std) {
        assertThat(world.cargo.getFlightId().flightNumber().value()).isEqualTo(flightNumber);
        world.departsAt(std);
    }

    @Given("{word} is booked on {word}")
    public void isBookedOn(String code, String flightNumber) {
        assertThat(world.cargo.getFlightId().flightNumber().value()).isEqualTo(flightNumber);
        world.book(new UldCode(code), ANY_WEIGHT);
    }

    @Given("{word} is also booked on {word}")
    public void isAlsoBookedOn(String code, String flightNumber) {
        isBookedOn(code, flightNumber);
    }

    @Given("it is {moment} UTC, within the loading period")
    public void itIsWithinTheLoadingPeriod(Instant moment) {
        itIs(moment);
        assertThat(world.cargo.loadingPeriod(CargoWorld.POLICY).contains(moment)).isTrue();
    }

    @Given("it is {moment} UTC")
    public void itIs(Instant moment) {
        world.now = moment;
    }

    @Given("the departure delays add up to {int} minutes")
    public void theDepartureDelaysAddUpTo(int minutes) {
        world.delayDepartureBy(Duration.ofMinutes(minutes));
    }

    @Given("{word} is already on {word}")
    public void isAlreadyOn(String code, String position) {
        world.book(new UldCode(code), ANY_WEIGHT);
        world.place(new UldCode(code), position);
    }

    /** A Given before the When (puts the ULD there) and a Then after it (checks that it is there). */
    @Given("{word} is on {word}")
    public void isOn(String code, String position) {
        if (world.acted) {
            assertThat(world.error).as("refusal").isNull();
            assertThat(world.uld(code).getPosition()).extracting(LoadPosition::name).isEqualTo(position);
        } else {
            world.place(new UldCode(code), position);
        }
    }

    @Given("the load has been finalized")
    public void theLoadHasBeenFinalized() {
        // The load checks are not part of these scenarios: a ULD on CL alone would fail the lateral balance.
        world.cargo.finalizeLoad(world.planner, world.now, List.of(), world.floorPlan, CargoWorld.POLICY);
    }

    /** A name that is not on the floor plan becomes a position without a side, which the aggregate refuses. */
    @When("I place {word} on {word}")
    public void iPlace(String code, String positionName) {
        LoadPosition position = world.floorPlan.positionNamed(positionName)
            .orElse(new LoadPosition(positionName, null));
        act(() -> assigned = world.cargo.assign(new UldCode(code), position, world.planner, world.now,
            world.floorPlan, CargoWorld.POLICY));
    }

    @When("I clear the load position of {word}")
    public void iClear(String code) {
        act(() -> cleared = world.cargo.clear(new UldCode(code), world.planner, world.now, CargoWorld.POLICY));
    }

    @Then("{word} is free")
    public void isFree(String position) {
        LoadPosition free = world.floorPlan.positionNamed(position).orElseThrow();
        assertThat(world.cargo.getUlds()).noneMatch(uld -> uld.isAt(free));
    }

    @Then("{word} no longer has a load position")
    public void noLongerHasALoadPosition(String code) {
        assertThat(world.error).as("refusal").isNull();
        assertThat(world.uld(code).hasPosition()).isFalse();
    }

    @Then("it is recorded that I assigned the load position")
    public void itIsRecordedThatIAssigned() {
        assertThat(assigned.assignedBy()).isEqualTo(world.planner);
        assertThat(world.uld(assigned.uldCode().value()).getPositionChangedBy()).isEqualTo(world.planner);
    }

    @Then("it is recorded that I cleared the load position")
    public void itIsRecordedThatICleared() {
        assertThat(cleared.clearedBy()).isEqualTo(world.planner);
        assertThat(world.uld(cleared.uldCode().value()).getPositionChangedBy()).isEqualTo(world.planner);
    }

    @Then("that is accepted")
    public void thatIsAccepted() {
        assertThat(world.error).as("refusal").isNull();
    }

    @Then("that is refused")
    public void thatIsRefused() {
        assertThat(world.error).as("refusal").isNotNull();
    }

    @Then("that is refused with an explanation")
    public void thatIsRefusedWithAnExplanation() {
        thatIsRefused();
        assertThat(world.error.getMessage()).isNotBlank();
    }

    private void act(Runnable action) {
        world.acted = true;
        try {
            action.run();
        } catch (DomainRuleViolationException e) {
            world.error = e;
        }
    }
}
