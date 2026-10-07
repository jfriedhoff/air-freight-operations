package be.kdg.airfreight.flightoperations.acceptance.unit;

import be.kdg.airfreight.flightoperations.domain.flight.Flight;
import be.kdg.airfreight.flightoperations.domain.flight.FlightStatus;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import static be.kdg.airfreight.flightoperations.acceptance.unit.FlightWorld.DEPARTURE_DATE;
import static be.kdg.airfreight.flightoperations.acceptance.unit.FlightWorld.HJ221;
import static be.kdg.airfreight.flightoperations.acceptance.unit.FlightWorld.NOW;
import static be.kdg.airfreight.flightoperations.acceptance.unit.FlightWorld.TOLERANCE;
import static be.kdg.airfreight.flightoperations.acceptance.unit.FlightWorld.onDate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class FlightDepartureSteps {
    private static final String NO_FINALIZED_LOAD = "once the load is finalized";
    private static final String ALREADY_REGISTERED = "already been registered";

    private final FlightWorld world;
    private Instant registeredAtd;

    public FlightDepartureSteps(FlightWorld world) {
        this.world = world;
    }

    @Given("flight {word} is planned with ETD {word} UTC")
    public void flightIsPlannedWithEtd(String flightNumber, String etd) {
        Instant departure = onDate(DEPARTURE_DATE, etd);
        world.plan(flightNumber, departure, departure.plus(Duration.ofHours(3)));
    }

    @Given("the finalized load has been received")
    public void theFinalizedLoadHasBeenReceived() {
        world.flight(HJ221).recordFinalizedLoad(NOW);
    }

    @Given("it is now {word} UTC")
    public void itIsNow(String time) {
        world.now = onDate(DEPARTURE_DATE, time);
    }

    @Given("the CPM has not been processed yet")
    public void theCpmHasNotBeenProcessedYet() {
        assertThat(world.flight(HJ221).isFinalizedLoadReceived()).isFalse();
    }

    @Given("my registration was refused because the CPM had not been processed yet")
    public void myRegistrationWasRefusedBecauseTheCpmHadNotBeenProcessedYet() {
        iRegisterAsTheAtd("06:50");
        theRegistrationIsRefusedBecauseTheFinalizedLoadHasNotBeenReceivedYet();
    }

    @Given("the CPM has been processed in the meantime")
    public void theCpmHasBeenProcessedInTheMeantime() {
        theFinalizedLoadHasBeenReceived();
    }

    @Given("the ATD has been registered")
    public void theAtdHasBeenRegistered() {
        Flight flight = world.flight(HJ221);
        Instant etd = flight.getEstimated().departure();
        flight.recordFinalizedLoad(etd);
        flight.registerDeparture(etd, world.planner, etd, TOLERANCE);
    }

    @When("I register {word} as the ATD")
    public void iRegisterAsTheAtd(String atd) {
        registeredAtd = onDate(DEPARTURE_DATE, atd);
        world.error = catchThrowable(() -> world.flight(HJ221).registerDeparture(registeredAtd, world.planner,
            world.now, TOLERANCE));
    }

    @When("I register {word} as the ATD again")
    public void iRegisterAsTheAtdAgain(String atd) {
        iRegisterAsTheAtd(atd);
    }

    /** Flight has no method to change an ATD: changing it means registering another ATD, which must be refused. */
    @When("I try to change the ATD")
    public void iTryToChangeTheAtd() {
        world.expectedRefusal = ALREADY_REGISTERED;
        Instant otherAtd = Objects.requireNonNull(world.flight(HJ221).getActualDeparture())
            .plus(Duration.ofMinutes(1));
        world.error = catchThrowable(() -> world.flight(HJ221).registerDeparture(otherAtd, world.planner, otherAtd,
            TOLERANCE));
    }

    @Then("the registration is refused with the explanation that the finalized load has not been received yet")
    public void theRegistrationIsRefusedBecauseTheFinalizedLoadHasNotBeenReceivedYet() {
        world.expectedRefusal = NO_FINALIZED_LOAD;
        world.assertRefusedForTheExpectedReason();
    }

    @And("the flight has departed")
    public void theFlightHasDeparted() {
        Flight flight = world.flight(HJ221);
        assertThat(flight.getStatus()).isEqualTo(FlightStatus.DEPARTED);
        assertThat(flight.getActualDeparture()).isEqualTo(registeredAtd);
    }

    @And("it is recorded that I registered the ATD")
    public void itIsRecordedThatIRegisteredTheAtd() {
        assertThat(world.flight(HJ221).getDepartureRegisteredBy()).isEqualTo(world.planner);
    }
}
