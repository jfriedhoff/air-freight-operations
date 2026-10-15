package be.kdg.airfreight.flightoperations.acceptance.integration;

import be.kdg.airfreight.flightoperations.TestClock;
import be.kdg.airfreight.flightoperations.TestHelper;
import be.kdg.airfreight.flightoperations.application.FlightRetentionService;
import be.kdg.airfreight.flightoperations.domain.flight.FlightId;
import be.kdg.airfreight.flightoperations.domain.flight.FlightStatus;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/** Story #38: time removes the flights whose retention period is over. */
public class FlightRetentionSteps {
    private static final String FLIGHT_REMOVED = "flight-operations.flight.removed";

    private final TestHelper testHelper;
    private final TestClock clock;
    private final FlightRetentionService retention;

    private FlightId flightId;

    public FlightRetentionSteps(TestHelper testHelper, TestClock clock, FlightRetentionService retention) {
        this.testHelper = testHelper;
        this.clock = clock;
        this.retention = retention;
    }

    @After
    public void resetClock() {
        clock.reset();
    }

    /** The schedule is the one TestHelper plans: 9 January 2026, STD 06:30 and STA 09:45 UTC. */
    @Given("flight HJ221 of 9 January 2026")
    public void flightHj221() {
        flightId = testHelper.savePlannedFlight().getId();
    }

    @Given("flight HJ221 of 9 January 2026 has {} and has a delay")
    public void flightHj221HasAndHasADelay(String state) {
        flightId = testHelper.savePlannedFlight().getId();
        testHelper.addDepartureDelay(flightId);
        FlightStatus expected = switch (state) {
            case "not departed yet" -> FlightStatus.PLANNED;
            case "departed" -> {
                testHelper.depart(flightId);
                yield FlightStatus.DEPARTED;
            }
            case "landed" -> {
                testHelper.depart(flightId);
                testHelper.land(flightId);
                yield FlightStatus.LANDED;
            }
            default -> throw new IllegalArgumentException("Unknown state: " + state);
        };
        assertThat(testHelper.find(flightId).getStatus()).isEqualTo(expected);
        assertThat(testHelper.find(flightId).getDelays()).hasSize(1);
    }

    /** At that moment the daily job runs, as FlightRetentionScheduler does at 00:00 UTC. */
    @When("it is {moment} UTC")
    public void itIs(Instant moment) {
        clock.set(moment);
        retention.removeExpiredFlights();
    }

    @Then("the flight and its delays have been removed")
    public void theFlightAndItsDelaysHaveBeenRemoved() {
        assertThat(testHelper.exists(flightId)).as("the flight exists").isFalse();
        assertThat(testHelper.delayCount()).as("delays in the database").isZero();
    }

    @Then("Flight Operations reports that the flight has been removed")
    public void flightOperationsReportsThatTheFlightHasBeenRemoved() {
        assertThat(testHelper.outboxTypes()).containsExactly(FLIGHT_REMOVED);
    }

    @Then("the flight still exists")
    public void theFlightStillExists() {
        assertThat(testHelper.exists(flightId)).as("the flight exists").isTrue();
        assertThat(testHelper.outboxTypes()).isEmpty();
    }
}
