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
import static be.kdg.airfreight.flightoperations.acceptance.unit.FlightWorld.TOLERANCE;
import static be.kdg.airfreight.flightoperations.acceptance.unit.FlightWorld.onDate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class FlightArrivalSteps {
    private static final String ETD = "06:45";
    private static final String ETA = "10:00";
    private static final String NOT_DEPARTED = "once the flight has departed";
    private static final String NOT_AFTER_ATD = "later than the ATD";
    private static final String ALREADY_REGISTERED = "already been registered";

    private final FlightWorld world;
    private Instant registeredAta;

    public FlightArrivalSteps(FlightWorld world) {
        this.world = world;
    }

    @Given("flight {word} is planned")
    public void flightIsPlanned(String flightNumber) {
        world.plan(flightNumber, onDate(DEPARTURE_DATE, ETD), onDate(DEPARTURE_DATE, ETA));
    }

    @Given("flight {word} has departed with ATD {word} UTC and has ETA {word} UTC")
    public void flightHasDepartedWithAtdAndEta(String flightNumber, String atd, String eta) {
        departed(flightNumber, atd, eta);
    }

    @Given("flight {word} has departed with ETA {word} UTC")
    public void flightHasDepartedWithEta(String flightNumber, String eta) {
        departed(flightNumber, ETD, eta);
    }

    @Given("the ATA has been registered")
    public void theAtaHasBeenRegistered() {
        departed(HJ221, ETD, ETA);
        registeredAta = onDate(DEPARTURE_DATE, ETA);
        world.flight(HJ221).registerArrival(registeredAta, world.planner, registeredAta, TOLERANCE);
    }

    @When("I register an ATA")
    public void iRegisterAnAta() {
        world.expectedRefusal = NOT_DEPARTED;
        Instant ata = onDate(DEPARTURE_DATE, ETA);
        world.error = catchThrowable(() -> world.flight(HJ221).registerArrival(ata, world.planner, ata, TOLERANCE));
    }

    @When("I register {word} as the ATA")
    public void iRegisterAsTheAta(String ata) {
        world.expectedRefusal = NOT_AFTER_ATD;
        registeredAta = onDate(DEPARTURE_DATE, ata);
        world.error = catchThrowable(() -> world.flight(HJ221).registerArrival(registeredAta, world.planner,
            world.now, TOLERANCE));
    }

    @Then("the flight has landed")
    public void theFlightHasLanded() {
        Flight flight = world.flight(HJ221);
        assertThat(flight.getStatus()).isEqualTo(FlightStatus.LANDED);
        assertThat(flight.getActualArrival()).isEqualTo(registeredAta);
    }

    /** Flight has no method to change an ATA: changing it means registering another ATA, which must be refused. */
    @And("the ATA can no longer be changed")
    public void theAtaCanNoLongerBeChanged() {
        Instant otherAta = Objects.requireNonNull(world.flight(HJ221).getActualArrival()).plus(Duration.ofMinutes(1));
        world.error = catchThrowable(() -> world.flight(HJ221).registerArrival(otherAta, world.planner, otherAta,
            TOLERANCE));
        world.expectedRefusal = ALREADY_REGISTERED;
        world.assertRefusedForTheExpectedReason();
        assertThat(world.flight(HJ221).getActualArrival()).isEqualTo(registeredAta);
    }

    @And("it is recorded who registered the ATA")
    public void itIsRecordedWhoRegisteredTheAta() {
        assertThat(world.flight(HJ221).getArrivalRegisteredBy()).isEqualTo(world.planner);
    }

    /** Plans the flight with ETD 06:45 and the given ETA, receives the CPM and registers the ATD. */
    private void departed(String flightNumber, String atd, String eta) {
        world.plan(flightNumber, onDate(DEPARTURE_DATE, ETD), onDate(DEPARTURE_DATE, eta));
        Instant departure = onDate(DEPARTURE_DATE, atd);
        Flight flight = world.flight(flightNumber);
        flight.recordFinalizedLoad(departure);
        flight.registerDeparture(departure, world.planner, departure, TOLERANCE);
    }
}
