package be.kdg.airfreight.flightoperations.acceptance.unit;

import be.kdg.airfreight.flightoperations.domain.flight.Delay;
import be.kdg.airfreight.flightoperations.domain.flight.DelayCode;
import be.kdg.airfreight.flightoperations.domain.flight.DelayDuration;
import be.kdg.airfreight.flightoperations.domain.flight.DelayId;
import be.kdg.airfreight.flightoperations.domain.flight.DelayKind;
import be.kdg.airfreight.flightoperations.domain.flight.EstimatedTimes;
import be.kdg.airfreight.flightoperations.domain.flight.Flight;
import be.kdg.airfreight.flightoperations.domain.flight.FlightEvent;
import be.kdg.airfreight.flightoperations.domain.flight.FlightNumber;
import be.kdg.airfreight.flightoperations.domain.flightplanner.FlightPlannerId;
import io.cucumber.java.en.And;
import io.cucumber.java.en.But;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import static be.kdg.airfreight.flightoperations.acceptance.unit.FlightWorld.DEPARTURE_DATE;
import static be.kdg.airfreight.flightoperations.acceptance.unit.FlightWorld.HJ221;
import static be.kdg.airfreight.flightoperations.acceptance.unit.FlightWorld.NOW;
import static be.kdg.airfreight.flightoperations.acceptance.unit.FlightWorld.TOLERANCE;
import static be.kdg.airfreight.flightoperations.acceptance.unit.FlightWorld.onDate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class FlightDelaySteps {
    private static final String VALID_REASON = "93";
    private static final String UNKNOWN_REASON = "XY";
    private static final String DESCRIPTION = "Late arrival of the aircraft";
    private static final String NEW_DESCRIPTION = "Waiting for a late crew member";
    private static final String TOO_LONG = "at most 12 hours";
    private static final String INVALID_REASON = "Delay reason 'XY' is invalid";
    private static final String NO_DESCRIPTION = "description of a delay cannot be blank";
    private static final String NOT_PLANNED = "only change while the flight is planned";
    private static final String LANDED = "no longer change once the flight has landed";

    private final FlightWorld world;
    private final FlightPlannerId colleague = FlightPlannerId.create();
    private DelayId delayId;
    private DelayId arrivalDelayId;
    private EstimatedTimes estimatedBefore;
    private List<FlightEvent> events = List.of();

    public FlightDelaySteps(FlightWorld world) {
        this.world = world;
    }

    @Given("flight {word} of {int} January {int} is planned with STD {word} and STA {word} UTC")
    public void flightIsPlannedWithStdAndSta(String flightNumber, int day, int year, String std, String sta) {
        world.plan(flightNumber, utc(day, year, std), utc(day, year, sta));
    }

    @Given("flight {word} of {int} January {int} is planned with STD {word} UTC")
    public void flightIsPlannedWithStd(String flightNumber, int day, int year, String std) {
        Instant departure = utc(day, year, std);
        world.plan(flightNumber, departure, departure.plus(Duration.ofHours(3)));
    }

    @Given("{word} already has a departure delay of {int} minutes")
    public void flightAlreadyHasADepartureDelay(String flightNumber, int minutes) {
        addDelay(flightNumber, DelayKind.DEPARTURE, minutes);
    }

    @Given("{word} has a departure delay of {int} minutes")
    public void flightHasADepartureDelay(String flightNumber, int minutes) {
        addDelay(flightNumber, DelayKind.DEPARTURE, minutes);
    }

    @Given("flight {word} with ETD {word} and ETA {word} UTC")
    public void flightWithEtdAndEta(String flightNumber, String etd, String eta) {
        world.plan(flightNumber, onDate(DEPARTURE_DATE, etd), onDate(DEPARTURE_DATE, eta));
    }

    @Given("flight {word} with STD {word}, STA {word} UTC and one departure delay of {int} minutes")
    public void flightWithOneDepartureDelay(String flightNumber, String std, String sta, int minutes) {
        world.plan(flightNumber, onDate(DEPARTURE_DATE, std), onDate(DEPARTURE_DATE, sta));
        Flight flight = world.flight(flightNumber);
        flight.addDelay(DelayKind.DEPARTURE, new DelayDuration(minutes), new DelayCode(VALID_REASON), DESCRIPTION,
            colleague, NOW);
        delayId = flight.getDelays().getLast().getId();
        estimatedBefore = flight.getEstimated();
    }

    @Given("{word} also has a(n) {word} delay of {int} minutes")
    public void flightAlsoHasADelay(String flightNumber, String kind, int minutes) {
        DelayId id = addDelay(flightNumber, kind(kind), minutes);
        if (kind(kind) == DelayKind.ARRIVAL) {
            arrivalDelayId = id;
        }
    }

    @Given("flight {word} has departed")
    public void flightHasDeparted(String flightNumber) {
        Flight flight = plannedFlight(flightNumber);
        Instant atd = flight.getScheduled().departure();
        flight.recordFinalizedLoad(atd);
        flight.registerDeparture(atd, world.planner, atd, TOLERANCE);
    }

    @Given("flight {word} has landed")
    public void flightHasLanded(String flightNumber) {
        flightHasDeparted(flightNumber);
        Flight flight = world.flight(flightNumber);
        Instant ata = flight.getScheduled().arrival();
        flight.registerArrival(ata, world.planner, ata, TOLERANCE);
    }

    @When("I enter a departure delay of {int} minutes with a valid delay reason and a description")
    public void iEnterAValidDepartureDelay(int minutes) {
        enterDelay(HJ221, DelayKind.DEPARTURE, minutes, VALID_REASON, DESCRIPTION);
    }

    @When("I enter a(n) {word} delay of {int} minutes")
    public void iEnterADelay(String kind, int minutes) {
        world.expectedRefusal = TOO_LONG;
        enterDelay(HJ221, kind(kind), minutes, VALID_REASON, DESCRIPTION);
    }

    @When("I enter another departure delay of {int} minutes and an arrival delay of {int} minutes")
    public void iEnterAnotherDepartureDelayAndAnArrivalDelay(int departureMinutes, int arrivalMinutes) {
        addDelay(HJ221, DelayKind.DEPARTURE, departureMinutes);
        addDelay(HJ221, DelayKind.ARRIVAL, arrivalMinutes);
    }

    @When("I enter a departure delay with a delay reason that is not in the IATA list")
    public void iEnterADepartureDelayWithAnUnknownReason() {
        world.expectedRefusal = INVALID_REASON;
        enterDelay(HJ221, DelayKind.DEPARTURE, 15, UNKNOWN_REASON, DESCRIPTION);
    }

    @When("I enter a departure delay without a description")
    public void iEnterADepartureDelayWithoutADescription() {
        world.expectedRefusal = NO_DESCRIPTION;
        enterDelay(HJ221, DelayKind.DEPARTURE, 15, VALID_REASON, "");
    }

    @When("I enter a departure delay of {int} minutes for {word}")
    public void iEnterADepartureDelayFor(int minutes, String flightNumber) {
        enterDelay(flightNumber, DelayKind.DEPARTURE, minutes, VALID_REASON, DESCRIPTION);
    }

    @When("I enter a departure delay")
    public void iEnterADepartureDelay() {
        world.expectedRefusal = NOT_PLANNED;
        enterDelay(HJ221, DelayKind.DEPARTURE, 15, VALID_REASON, DESCRIPTION);
    }

    @When("I enter an arrival delay")
    public void iEnterAnArrivalDelay() {
        world.expectedRefusal = LANDED;
        enterDelay(HJ221, DelayKind.ARRIVAL, 15, VALID_REASON, DESCRIPTION);
    }

    @When("I change the delay to {int} minutes")
    public void iChangeTheDelayTo(int minutes) {
        changeDelay(delayId, minutes, DESCRIPTION);
    }

    @When("I change the departure delay to {int} minutes")
    public void iChangeTheDepartureDelayTo(int minutes) {
        world.expectedRefusal = TOO_LONG;
        changeDelay(delayId, minutes, DESCRIPTION);
    }

    @When("I change the description of the delay")
    public void iChangeTheDescriptionOfTheDelay() {
        changeDelay(delayId, delay(delayId).getMinutes().minutes(), NEW_DESCRIPTION);
    }

    @When("I remove the delay")
    public void iRemoveTheDelay() {
        removeDelay(delayId);
    }

    @When("I remove the delay of {int} minutes")
    public void iRemoveTheDelayOf(int minutes) {
        removeDelay(world.flight(HJ221).getDelays().stream()
            .filter(delay -> delay.getMinutes().minutes() == minutes)
            .findFirst().orElseThrow().getId());
    }

    @When("I try to {word} the departure delay")
    public void iTryToChangeOrRemoveTheDepartureDelay(String action) {
        world.expectedRefusal = NOT_PLANNED;
        changeOrRemove(action, delayId);
    }

    @When("I {word} the arrival delay")
    public void iChangeOrRemoveTheArrivalDelay(String action) {
        world.expectedRefusal = LANDED;
        changeOrRemove(action, arrivalDelayId);
    }

    @Then("the ETD is {word}")
    public void theEtdIs(String time) {
        Flight flight = world.flight(HJ221);
        assertThat(flight.getEstimated().departure()).isEqualTo(onDepartureDate(flight, time));
    }

    @Then("the ETD remains {word}")
    public void theEtdRemains(String time) {
        assertThat(world.error).isNull();
        theEtdIs(time);
    }

    @And("the ETA is {word}")
    public void theEtaIs(String time) {
        Flight flight = world.flight(HJ221);
        assertThat(flight.getEstimated().arrival()).isEqualTo(onDepartureDate(flight, time));
    }

    @And("it is recorded that I entered the delay")
    public void itIsRecordedThatIEnteredTheDelay() {
        assertThat(world.flight(HJ221).getDelays().getLast().getLastChangedBy()).isEqualTo(world.planner);
    }

    @And("it is recorded that I last changed the delay")
    public void itIsRecordedThatILastChangedTheDelay() {
        assertThat(world.error).isNull();
        assertThat(delay(delayId).getLastChangedBy()).isEqualTo(world.planner);
    }

    @Then("the delay has the new description")
    public void theDelayHasTheNewDescription() {
        assertThat(world.error).isNull();
        assertThat(delay(delayId).getDescription()).isEqualTo(NEW_DESCRIPTION);
    }

    /** The domain has no way to change the reason (Delay.reason is final): a change keeps it. */
    @Then("the delay keeps its delay reason")
    public void theDelayKeepsItsDelayReason() {
        assertThat(world.error).isNull();
        assertThat(delay(delayId).getReason()).isEqualTo(new DelayCode(VALID_REASON));
    }

    @And("the ETD and the ETA remain unchanged")
    public void theEtdAndTheEtaRemainUnchanged() {
        assertThat(world.flight(HJ221).getEstimated()).isEqualTo(estimatedBefore);
    }

    @Then("the ETD equals the STD again")
    public void theEtdEqualsTheStdAgain() {
        assertThat(world.error).isNull();
        Flight flight = world.flight(HJ221);
        assertThat(flight.getEstimated().departure()).isEqualTo(flight.getScheduled().departure());
    }

    @And("the ETA equals the STA again")
    public void theEtaEqualsTheStaAgain() {
        Flight flight = world.flight(HJ221);
        assertThat(flight.getEstimated().arrival()).isEqualTo(flight.getScheduled().arrival());
    }

    /** Flight keeps no history of removed delays: the removal is recorded in the estimate events it returns. */
    @And("it is recorded that I removed the delay")
    public void itIsRecordedThatIRemovedTheDelay() {
        assertThat(events).isNotEmpty().extracting("changedBy").containsOnly(world.planner);
    }

    @Then("the ETD of {word} is {int} January {int} {word} UTC")
    public void theEtdOfFlightIs(String flightNumber, int day, int year, String time) {
        assertThat(world.error).isNull();
        assertThat(world.flight(flightNumber).getEstimated().departure()).isEqualTo(utc(day, year, time));
    }

    @But("it remains the flight {word} of {int} January {int}")
    public void itRemainsTheFlight(String flightNumber, int day, int year) {
        Flight flight = world.flight(flightNumber);
        assertThat(flight.getFlightNumber()).isEqualTo(new FlightNumber(flightNumber));
        assertThat(flight.getScheduled().departureDate()).isEqualTo(LocalDate.of(year, Month.JANUARY, day));
    }

    /** Story 9 starts without a Background: plan the flight like story 8 does if it doesn't exist yet. */
    private Flight plannedFlight(String flightNumber) {
        if (world.flight(flightNumber) == null) {
            world.plan(flightNumber, onDate(DEPARTURE_DATE, "06:30"), onDate(DEPARTURE_DATE, "09:45"));
        }
        return world.flight(flightNumber);
    }

    private DelayId addDelay(String flightNumber, DelayKind kind, int minutes) {
        Flight flight = world.flight(flightNumber);
        flight.addDelay(kind, new DelayDuration(minutes), new DelayCode(VALID_REASON), DESCRIPTION, world.planner,
            NOW);
        return flight.getDelays().getLast().getId();
    }

    private void enterDelay(String flightNumber, DelayKind kind, int minutes, String reason, String description) {
        world.error = catchThrowable(() -> world.flight(flightNumber).addDelay(kind, new DelayDuration(minutes),
            new DelayCode(reason), description, world.planner, NOW));
    }

    private void changeDelay(DelayId id, int minutes, String description) {
        world.error = catchThrowable(() -> events = world.flight(HJ221).changeDelay(id, new DelayDuration(minutes),
            description, world.planner, NOW));
    }

    private void removeDelay(DelayId id) {
        world.error = catchThrowable(() -> events = world.flight(HJ221).removeDelay(id, world.planner, NOW));
    }

    private void changeOrRemove(String action, DelayId id) {
        switch (action) {
            case "change" -> changeDelay(id, delay(id).getMinutes().minutes() + 5, DESCRIPTION);
            case "remove" -> removeDelay(id);
            default -> throw new IllegalArgumentException("Unknown action: " + action);
        }
    }

    private Delay delay(DelayId id) {
        return world.flight(HJ221).getDelays().stream()
            .filter(delay -> delay.getId().equals(id))
            .findFirst().orElseThrow();
    }

    private static DelayKind kind(String kind) {
        return DelayKind.valueOf(kind.toUpperCase());
    }

    private static Instant utc(int day, int year, String time) {
        return onDate(LocalDate.of(year, Month.JANUARY, day), time);
    }

    private static Instant onDepartureDate(Flight flight, String time) {
        return onDate(flight.getScheduled().departureDate(), time);
    }
}
