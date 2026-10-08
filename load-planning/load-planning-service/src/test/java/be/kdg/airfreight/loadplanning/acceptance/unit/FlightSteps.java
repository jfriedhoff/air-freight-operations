package be.kdg.airfreight.loadplanning.acceptance.unit;

import io.cucumber.java.en.Given;

import java.time.Duration;
import java.time.Instant;

/** The flight and the moment of the scenario. */
public class FlightSteps {
    private static final String OUTSIDE_LOADING_PERIOD = "only change within the loading period";

    private final CargoWorld world;

    public FlightSteps(CargoWorld world) {
        this.world = world;
    }

    @Given("flight {word} with STD {moment} UTC")
    public void flightWithStd(String flightNumber, Instant std) {
        world.assertFlight(flightNumber);
        world.departsAt(std);
    }

    @Given("the flight goes to {word}")
    public void theFlightGoesTo(String airport) {
        world.goesTo(airport);
    }

    @Given("{word} goes to {word} and it is within the loading period")
    public void goesToAndItIsWithinTheLoadingPeriod(String flightNumber, String airport) {
        world.assertFlight(flightNumber);
        world.goesTo(airport);
        world.assertWithinLoadingPeriod();
    }

    @Given("the departure delays add up to {int} minutes")
    public void theDepartureDelaysAddUpTo(int minutes) {
        world.delayDepartureBy(Duration.ofMinutes(minutes));
    }

    @Given("it is {moment} UTC, within the loading period")
    public void itIsWithinTheLoadingPeriod(Instant moment) {
        world.now = moment;
        world.assertWithinLoadingPeriod();
    }

    /** A scenario names the moment to test the loading period: a refusal then has to be for that reason. */
    @Given("it is {moment} UTC")
    public void itIs(Instant moment) {
        world.now = moment;
        world.expectedRefusal = OUTSIDE_LOADING_PERIOD;
    }
}
