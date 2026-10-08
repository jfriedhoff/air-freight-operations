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
import static be.kdg.airfreight.flightoperations.acceptance.unit.FlightWorld.STA;
import static be.kdg.airfreight.flightoperations.acceptance.unit.FlightWorld.STD;
import static be.kdg.airfreight.flightoperations.acceptance.unit.FlightWorld.TOLERANCE;
import static be.kdg.airfreight.flightoperations.acceptance.unit.FlightWorld.onDepartureDate;
import static org.assertj.core.api.Assertions.assertThat;

/** Registering the ATA. */
public class FlightArrivalSteps {
    private static final String NOT_DEPARTED = "once the flight has departed";
    private static final String NOT_AFTER_ATD = "later than the ATD";
    private static final String ALREADY_REGISTERED = "already been registered";

    private final FlightWorld world;
    private Instant registeredAta;

    public FlightArrivalSteps(FlightWorld world) {
        this.world = world;
    }

    /** The flight departed at its STD and landed at its STA. */
    @Given("the ATA has been registered")
    public void theAtaHasBeenRegistered() {
        world.plan(HJ221);
        world.depart(HJ221, onDepartureDate(STD));
        registeredAta = onDepartureDate(STA);
        world.land(HJ221, registeredAta);
    }

    @When("I register an ATA")
    public void iRegisterAnAta() {
        world.expectedRefusal = NOT_DEPARTED;
        Instant ata = onDepartureDate(STA);
        world.attempt(() -> world.flight(HJ221).registerArrival(ata, world.planner, ata, TOLERANCE));
    }

    @When("I register {time} as the ATA")
    public void iRegisterAsTheAta(LocalTime ata) {
        world.expectedRefusal = NOT_AFTER_ATD;
        registeredAta = onDepartureDate(ata);
        world.attempt(() -> world.flight(HJ221).registerArrival(registeredAta, world.planner, world.now, TOLERANCE));
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
        world.expectedRefusal = ALREADY_REGISTERED;
        world.attempt(() -> world.flight(HJ221).registerArrival(otherAta, world.planner, otherAta, TOLERANCE));
        world.assertRefusedForTheExpectedReason();
        assertThat(world.flight(HJ221).getActualArrival()).isEqualTo(registeredAta);
    }

    @And("it is recorded who registered the ATA")
    public void itIsRecordedWhoRegisteredTheAta() {
        assertThat(world.flight(HJ221).getArrivalRegisteredBy()).isEqualTo(world.planner);
    }
}
