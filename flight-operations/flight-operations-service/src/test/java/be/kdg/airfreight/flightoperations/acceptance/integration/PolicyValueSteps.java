package be.kdg.airfreight.flightoperations.acceptance.integration;

import be.kdg.airfreight.flightoperations.FlightOperationsApplication;
import be.kdg.airfreight.flightoperations.TestClock;
import be.kdg.airfreight.flightoperations.TestHelper;
import be.kdg.airfreight.flightoperations.application.FlightRetentionService;
import be.kdg.airfreight.flightoperations.application.FlightService;
import be.kdg.airfreight.flightoperations.application.command.RegisterDepartureCommand;
import be.kdg.airfreight.flightoperations.domain.flight.FlightId;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Story #37: a policy value comes from the configuration. "Restarted without a new build" is a second
 * application context of the same classes, started with only that value changed, as a command-line argument
 * (external configuration wins over application.properties), on a free port. The steps use the use cases
 * of that restarted unit; it shares the test database with the scenario.
 */
public class PolicyValueSteps {
    private static final LocalDate DATE = LocalDate.of(2026, 1, 9);

    private final TestHelper testHelper;

    private String property;
    private ConfigurableApplicationContext restarted;
    private FlightId flightId;
    private Throwable error;

    public PolicyValueSteps(TestHelper testHelper) {
        this.testHelper = testHelper;
    }

    @After
    public void stopTheRestartedUnit() {
        if (restarted != null) {
            restarted.close();
        }
    }

    @Given("{} has been changed to {} in the external configuration")
    public void hasBeenChangedTo(String value, String newValue) {
        property = switch (value) {
            case "the tolerance for actual times" ->
                "--flight-operations.policy.actual-time-tolerance=" + newValue.replace(" min", "m");
            case "the retention period" -> "--flight-operations.policy.retention-days=" + newValue.replace(" days", "");
            default -> throw new IllegalArgumentException("Unknown policy value: " + value);
        };
    }

    @Given("the unit has been restarted without a new build")
    public void theUnitHasBeenRestarted() {
        restarted = new SpringApplicationBuilder(FlightOperationsApplication.class, TestClock.class)
            .run(property, "--server.port=0");
    }

    /** The load is finalized (a CPM was processed) and it is a minute after the ATD. */
    @When("the flight planner registers {time} as the ATD with an ETD of {time} UTC")
    public void theFlightPlannerRegistersTheAtd(LocalTime atd, LocalTime etd) {
        testHelper.saveFlight("HJ221", "BRU", DATE, etd, "SXBAA");
        flightId = testHelper.findFlight("HJ221", DATE, "BRU").orElseThrow().getId();
        testHelper.recordFinalizedLoad(flightId);
        restarted.getBean(TestClock.class).set(utc(DATE, atd.plusMinutes(1)));
        error = catchThrowable(() -> restarted.getBean(FlightService.class)
            .registerDeparture(new RegisterDepartureCommand(flightId, utc(DATE, atd), testHelper.saveFlightPlanner())));
    }

    /** At midnight the daily job of the restarted unit runs. */
    @When("it becomes {date}")
    public void itBecomes(LocalDate date) {
        testHelper.saveFlight("HJ221", "BRU", DATE, LocalTime.of(6, 30), "SXBAA");
        restarted.getBean(TestClock.class).set(utc(date, LocalTime.MIDNIGHT));
        restarted.getBean(FlightRetentionService.class).removeExpiredFlights();
    }

    @Then("the registration is accepted")
    public void theRegistrationIsAccepted() {
        assertThat(error).as("refusal").isNull();
        assertThat(testHelper.find(flightId).getActualDeparture()).isNotNull();
    }

    @Then("the flight of {date} still exists")
    public void theFlightStillExists(LocalDate date) {
        assertThat(testHelper.findFlight("HJ221", date, "BRU")).isPresent();
    }

    private static Instant utc(LocalDate date, LocalTime time) {
        return date.atTime(time).toInstant(ZoneOffset.UTC);
    }
}
