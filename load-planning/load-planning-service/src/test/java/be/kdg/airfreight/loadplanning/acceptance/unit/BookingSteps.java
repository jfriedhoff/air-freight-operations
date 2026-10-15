package be.kdg.airfreight.loadplanning.acceptance.unit;

import be.kdg.airfreight.loadplanning.acceptance.common.Weights;
import be.kdg.airfreight.loadplanning.domain.flightcargo.UldCode;
import be.kdg.airfreight.loadplanning.domain.flightcargo.Weight;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.time.LocalDate;

import static be.kdg.airfreight.loadplanning.acceptance.unit.CargoWorld.ANY_WEIGHT;
import static org.assertj.core.api.Assertions.assertThat;

/** Cargo that is booked on a flight. */
public class BookingSteps {
    private static final String ALREADY_BOOKED = "is already booked on this flight";
    private static final String NO_WEIGHT = "A weight must be greater than 0";

    private final CargoWorld world;
    /** The ULD that the When step booked. */
    private UldCode arrived;

    public BookingSteps(CargoWorld world) {
        this.world = world;
    }

    /** Given and When: these scenarios expect the booking to succeed, so a refusal fails them right away. */
    @Given("{uld} is booked on {word} of {date}")
    public void isBookedOnFlightOf(UldCode code, String flightNumber, LocalDate date) {
        world.bookOn(flightNumber, date, code);
    }

    @Given("{uld} is booked with {weight} on {word} of {date}")
    public void isBookedWithOn(UldCode code, String weight, String flightNumber, LocalDate date) {
        world.bookOn(flightNumber, date, code, weight);
    }

    @When("{uld} is booked again with {weight} on {word} of {date}")
    public void isBookedAgainWithOn(UldCode code, String weight, String flightNumber, LocalDate date) {
        world.expectedRefusal = ALREADY_BOOKED;
        world.attempt(() -> world.bookOn(flightNumber, date, code, weight));
    }

    @When("{uld} of {weight} is booked on {word} of {date}")
    public void ofWeightIsBookedOn(UldCode code, String weight, String flightNumber, LocalDate date) {
        world.expectedRefusal = NO_WEIGHT;
        world.attempt(() -> world.bookOn(flightNumber, date, code, weight));
    }

    @Then("there is still one booking of {uld} with {weight}")
    public void thereIsStillOneBookingOf(UldCode code, String weight) {
        Weight expected = Weights.of(weight);
        assertThat(world.cargo.getUlds())
            .filteredOn(uld -> uld.getCode().equals(code))
            .singleElement()
            .satisfies(uld -> {
                assertThat(uld.getWeight().value()).isEqualByComparingTo(expected.value());
                assertThat(uld.getWeight().unit()).isEqualTo(expected.unit());
            });
    }

    @Then("{uld} is booked on both flights")
    public void isBookedOnBothFlights(UldCode code) {
        assertThat(world.allFlights()).hasSize(2).allMatch(flight -> flight.hasBooked(code));
    }

    @Given("{uld} is booked on {word}")
    @Given("{uld} is also booked on {word}")
    public void isBookedOn(UldCode code, String flightNumber) {
        world.assertFlight(flightNumber);
        world.book(code, ANY_WEIGHT);
    }

    /** A separate When step, because "{uld} is booked on {word}" is already a Given in story 18. */
    @When("a booking of {uld} on {word} arrives")
    public void aBookingArrives(UldCode code, String flightNumber) {
        world.assertFlight(flightNumber);
        arrived = code;
        world.attempt(() -> world.book(code, ANY_WEIGHT));
    }

    /** Stories 22 and 23 use this step to book a ULD. Story 15 uses it to check that the booking worked. */
    @Given("{uld} is booked but has no load position")
    @Given("{uld} is booked without a load position")
    public void isBookedWithoutALoadPosition(UldCode code) {
        if (world.acted) {
            world.assertAccepted();
            assertThat(world.uld(code).hasPosition()).isFalse();
        } else {
            world.book(code, ANY_WEIGHT);
        }
    }

    /** The step that finalized the load has set the expected refusal. */
    @Then("no load position can be assigned to it anymore")
    public void noLoadPositionCanBeAssignedToItAnymore() {
        world.attempt(() -> world.place(arrived, world.freePosition()));
        world.assertRefusedForTheExpectedReason();
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

    @Given("no cargo is booked on {word}")
    public void noCargoIsBookedOn(String flightNumber) {
        world.assertFlight(flightNumber);
        assertThat(world.cargo.getUlds()).isEmpty();
    }
}
