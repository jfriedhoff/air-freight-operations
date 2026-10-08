package be.kdg.airfreight.loadplanning.acceptance.unit;

import be.kdg.airfreight.loadplanning.domain.flightcargo.UldCode;
import io.cucumber.java.en.Given;

import static org.assertj.core.api.Assertions.assertThat;

/** Story 23: the Givens only this story words this way. */
public class CheckLoadSteps {
    private final CargoWorld world;

    public CheckLoadSteps(CargoWorld world) {
        this.world = world;
    }

    @Given("{word} goes to {word} and it is within the loading period")
    public void goesToAndItIsWithinTheLoadingPeriod(String flightNumber, String airport) {
        assertThat(world.cargo.getFlightId().flightNumber().value()).isEqualTo(flightNumber);
        world.goesTo(airport);
        assertThat(world.cargo.loadingPeriod(CargoWorld.POLICY).contains(world.now)).isTrue();
    }

    @Given("{word}, {weight} for {word}, is on {word} and {word}, {weight} for {word}, is on {word}")
    public void twoUldsAreOn(String firstCode, String firstWeight, String firstDestination, String firstPosition,
                             String secondCode, String secondWeight, String secondDestination,
                             String secondPosition) {
        world.loadFor(new UldCode(firstCode), firstWeight, firstDestination, firstPosition);
        world.loadFor(new UldCode(secondCode), secondWeight, secondDestination, secondPosition);
    }
}
