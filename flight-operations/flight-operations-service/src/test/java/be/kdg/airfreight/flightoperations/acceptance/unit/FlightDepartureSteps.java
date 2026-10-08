package be.kdg.airfreight.flightoperations.acceptance.unit;

import be.kdg.airfreight.flightoperations.domain.flight.Flight;
import be.kdg.airfreight.flightoperations.domain.flight.FlightStatus;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Objects;

import static be.kdg.airfreight.flightoperations.acceptance.unit.FlightWorld.HJ221;
import static be.kdg.airfreight.flightoperations.acceptance.unit.FlightWorld.TOLERANCE;
import static be.kdg.airfreight.flightoperations.acceptance.unit.FlightWorld.onDepartureDate;
import static org.assertj.core.api.Assertions.assertThat;

/** The finalized load (CPM) and registering the ATD. */
public class FlightDepartureSteps {
    /** Within 5 minutes of the ETD 06:45 and before now: only a missing CPM can refuse it. */
    private static final LocalTime VALID_ATD = LocalTime.of(6, 50);
    private static final String NO_FINALIZED_LOAD = "once the load is finalized";
    private static final String ALREADY_REGISTERED = "already been registered";

    private final FlightWorld world;
    private Instant registeredAtd;

    public FlightDepartureSteps(FlightWorld world) {
        this.world = world;
    }

    @Given("the finalized load has been received")
    @Given("the CPM has been processed in the meantime")
    public void theFinalizedLoadHasBeenReceived() {
        world.flight(HJ221).recordFinalizedLoad(world.now);
    }

    @Given("the CPM has not been processed yet")
    public void theCpmHasNotBeenProcessedYet() {
        assertThat(world.flight(HJ221).isFinalizedLoadReceived()).isFalse();
    }

    @Given("my registration was refused because the CPM had not been processed yet")
    public void myRegistrationWasRefusedBecauseTheCpmHadNotBeenProcessedYet() {
        register(VALID_ATD);
        assertRefusedWithoutAFinalizedLoad();
    }

    /** At the ETD. */
    @Given("the ATD has been registered")
    public void theAtdHasBeenRegistered() {
        world.depart(HJ221, world.flight(HJ221).getEstimated().departure());
    }

    @When("I register {time} as the ATD")
    @When("I register {time} as the ATD again")
    public void iRegisterAsTheAtd(LocalTime atd) {
        register(atd);
    }

    /** Flight has no method to change an ATD: changing it means registering another ATD, which must be refused. */
    @When("I try to change the ATD")
    public void iTryToChangeTheAtd() {
        world.expectedRefusal = ALREADY_REGISTERED;
        Instant otherAtd = Objects.requireNonNull(world.flight(HJ221).getActualDeparture())
            .plus(Duration.ofMinutes(1));
        world.attempt(() -> world.flight(HJ221).registerDeparture(otherAtd, world.planner, otherAtd, TOLERANCE));
    }

    @Then("the registration is refused with the explanation that the finalized load has not been received yet")
    public void theRegistrationIsRefusedWithoutAFinalizedLoad() {
        assertRefusedWithoutAFinalizedLoad();
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

    private void register(LocalTime atd) {
        registeredAtd = onDepartureDate(atd);
        world.attempt(() -> world.flight(HJ221).registerDeparture(registeredAtd, world.planner, world.now,
            TOLERANCE));
    }

    private void assertRefusedWithoutAFinalizedLoad() {
        world.expectedRefusal = NO_FINALIZED_LOAD;
        world.assertRefusedForTheExpectedReason();
    }
}
