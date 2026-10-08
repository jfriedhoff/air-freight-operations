package be.kdg.airfreight.loadplanning.acceptance.integration;

import be.kdg.airfreight.loadplanning.LoadPlanningApplication;
import be.kdg.airfreight.loadplanning.acceptance.common.Weights;
import be.kdg.airfreight.loadplanning.application.LoadService;
import be.kdg.airfreight.loadplanning.application.command.AssignLoadPositionCommand;
import be.kdg.airfreight.loadplanning.domain.DomainRuleViolationException;
import be.kdg.airfreight.loadplanning.domain.flightcargo.AirportCode;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargo;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargoRepository;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightId;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightInfo;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightNumber;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadCheckResult;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadingPolicy;
import be.kdg.airfreight.loadplanning.domain.flightcargo.UldCode;
import be.kdg.airfreight.loadplanning.domain.loadplanner.LoadPlannerId;
import be.kdg.airfreight.loadplanning.infrastructure.floorplan.Boeing747400FFloorPlan;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.time.Duration;
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
    private static final UldCode ULD = new UldCode("AKE16689R7");
    private static final String OUTSIDE_LOADING_PERIOD = "only change within the loading period";

    private final FlightCargoRepository cargos;

    private String property;
    private ConfigurableApplicationContext restarted;
    private Throwable error;
    private LoadCheckResult lateralBalance;

    public PolicyValueSteps(FlightCargoRepository cargos) {
        this.cargos = cargos;
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
            case "the start of the loading period" ->
                "--load-planning.policy.loading-opens-before-departure=" + newValue.replace(" h", "h");
            case "the end of the loading period" ->
                "--load-planning.policy.loading-closes-before-departure=" + newValue.replace(" min", "m");
            case "the allowed lateral balance difference" -> "--load-planning.policy.lateral-balance-limit="
                + Integer.parseInt(newValue.replace("%", "")) / 100.0;
            default -> throw new IllegalArgumentException("Unknown policy value: " + value);
        };
    }

    /** The test profile creates the schema and drops it on close: the restarted unit must leave it alone. */
    @Given("the unit has been restarted without a new build")
    public void theUnitHasBeenRestarted() {
        restarted = new SpringApplicationBuilder(LoadPlanningApplication.class, TestClock.class)
            .run(property, "--server.port=0", "--spring.jpa.hibernate.ddl-auto=none");
    }

    @When("the load planner assigns a load position at {time} the evening before, with an STD of {time} UTC")
    public void assignsTheEveningBefore(LocalTime at, LocalTime std) {
        assign(DATE.minusDays(1).atTime(at).toInstant(ZoneOffset.UTC), std);
    }

    @When("the load planner assigns a load position at {time}, with an STD of {time} UTC")
    public void assigns(LocalTime at, LocalTime std) {
        assign(DATE.atTime(at).toInstant(ZoneOffset.UTC), std);
    }

    /** The load is put on CL and CR beforehand; the restarted unit checks it with its own limit. */
    @When("the lateral balance is checked for {weight} left and {weight} right")
    public void theLateralBalanceIsChecked(String left, String right) {
        FlightCargo cargo = flightWithStd(LocalTime.of(6, 30));
        Instant withinTheLoadingPeriod = DATE.atTime(5, 0).toInstant(ZoneOffset.UTC);
        LoadingPolicy anyPolicy = new LoadingPolicy(Duration.ofHours(12), Duration.ofMinutes(20));
        var floorPlan = Boeing747400FFloorPlan.build();
        cargo.book(ULD, Weights.of(left), new AirportCode("ATH"), withinTheLoadingPeriod);
        cargo.book(new UldCode("AKE16690R7"), Weights.of(right), new AirportCode("ATH"), withinTheLoadingPeriod);
        cargo.assign(ULD, floorPlan.positionNamed("CL").orElseThrow(), LoadPlannerId.create(),
            withinTheLoadingPeriod, floorPlan, anyPolicy);
        cargo.assign(new UldCode("AKE16690R7"), floorPlan.positionNamed("CR").orElseThrow(), LoadPlannerId.create(),
            withinTheLoadingPeriod, floorPlan, anyPolicy);
        cargos.save(cargo);
        lateralBalance = restarted.getBean(LoadService.class).checkLoad(cargo.getId()).results().stream()
            .filter(result -> result.checkName().equals("Lateral balance"))
            .findFirst()
            .orElseThrow();
    }

    @Then("the assignment is refused")
    public void theAssignmentIsRefused() {
        assertThat(error).isInstanceOf(DomainRuleViolationException.class).hasMessageContaining(OUTSIDE_LOADING_PERIOD);
    }

    @Then("the check has passed")
    public void theCheckHasPassed() {
        assertThat(lateralBalance.passed()).withFailMessage("The check failed: %s", lateralBalance.reason()).isTrue();
    }

    /** A flight with one booked ULD; the restarted unit assigns it at that moment. */
    private void assign(Instant at, LocalTime std) {
        FlightCargo cargo = flightWithStd(std);
        cargo.book(ULD, Weights.of("1000 kg"), new AirportCode("ATH"), at);
        cargos.save(cargo);
        restarted.getBean(TestClock.class).set(at);
        error = catchThrowable(() -> restarted.getBean(LoadService.class)
            .assign(new AssignLoadPositionCommand(cargo.getId(), ULD, "CL", LoadPlannerId.create())));
    }

    /** HJ221 of 9 January 2026 from BRU to ATH, 3 h 15 min. */
    private static FlightCargo flightWithStd(LocalTime std) {
        Instant departure = DATE.atTime(std).toInstant(ZoneOffset.UTC);
        return FlightCargo.create(new FlightId(new FlightNumber("HJ221"), DATE, new AirportCode("BRU")),
            FlightInfo.scheduled(new AirportCode("ATH"), Boeing747400FFloorPlan.AIRCRAFT_TYPE, departure,
                departure.plus(Duration.ofMinutes(195))));
    }
}
