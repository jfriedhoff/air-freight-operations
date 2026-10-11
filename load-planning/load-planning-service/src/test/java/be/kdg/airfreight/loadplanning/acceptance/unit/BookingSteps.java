package be.kdg.airfreight.loadplanning.acceptance.unit;

import be.kdg.airfreight.loadplanning.domain.flightcargo.UldCode;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;

import java.time.LocalDate;

import static be.kdg.airfreight.loadplanning.acceptance.unit.CargoWorld.ANY_WEIGHT;
import static org.assertj.core.api.Assertions.assertThat;

/** Cargo that is booked on a flight. */
public class BookingSteps {
    private final CargoWorld world;

    public BookingSteps(CargoWorld world) {
        this.world = world;
    }

    /** Given and When: these scenarios expect the booking to succeed, so a refusal fails them right away. */
    @Given("{uld} is booked on {word} of {date}")
    public void isBookedOnFlightOf(UldCode code, String flightNumber, LocalDate date) {
        world.bookOn(flightNumber, date, code);
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

    @Given("no cargo is booked on {word}")
    public void noCargoIsBookedOn(String flightNumber) {
        world.assertFlight(flightNumber);
        assertThat(world.cargo.getUlds()).isEmpty();
    }
}
