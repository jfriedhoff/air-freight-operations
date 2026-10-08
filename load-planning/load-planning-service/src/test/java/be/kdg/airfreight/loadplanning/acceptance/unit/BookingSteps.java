package be.kdg.airfreight.loadplanning.acceptance.unit;

import be.kdg.airfreight.loadplanning.domain.flightcargo.UldCode;
import io.cucumber.java.en.Given;

import static be.kdg.airfreight.loadplanning.acceptance.unit.CargoWorld.ANY_WEIGHT;

/** Cargo that is booked on the flight but has no load position. */
public class BookingSteps {
    private final CargoWorld world;

    public BookingSteps(CargoWorld world) {
        this.world = world;
    }

    @Given("{uld} is booked on {word}")
    @Given("{uld} is also booked on {word}")
    public void isBookedOn(UldCode code, String flightNumber) {
        world.assertFlight(flightNumber);
        world.book(code, ANY_WEIGHT);
    }

    /** Story 15 uses "is booked without a load position" as a Then: add the acted check of isOn then. */
    @Given("{uld} is booked but has no load position")
    @Given("{uld} is booked without a load position")
    public void isBookedWithoutALoadPosition(UldCode code) {
        world.book(code, ANY_WEIGHT);
    }

    @Given("{uld} of {weight} is booked without a load position")
    public void ofWeightIsBookedWithoutALoadPosition(UldCode code, String weight) {
        world.book(code, weight);
    }

    @Given("no cargo is booked on the flight")
    @Given("no cargo on board")
    public void noCargo() {
        // the flight starts without cargo
    }
}
