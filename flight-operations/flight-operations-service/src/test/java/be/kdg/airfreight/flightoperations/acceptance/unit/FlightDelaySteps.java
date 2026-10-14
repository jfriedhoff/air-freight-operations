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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static be.kdg.airfreight.flightoperations.acceptance.unit.FlightWorld.HJ221;
import static be.kdg.airfreight.flightoperations.acceptance.unit.FlightWorld.NOW;
import static be.kdg.airfreight.flightoperations.acceptance.unit.FlightWorld.onDate;
import static be.kdg.airfreight.flightoperations.acceptance.unit.FlightWorld.onDepartureDate;
import static org.assertj.core.api.Assertions.assertThat;

/** Entering, changing and removing delays, and the ETD and ETA they give. */
public class FlightDelaySteps {
    private static final String VALID_REASON = "93";
    /** Well-formed, but IATA does not use it. */
    private static final String UNKNOWN_REASON = "50";
    private static final String DESCRIPTION = "Late arrival of the aircraft";
    private static final String NEW_DESCRIPTION = "Waiting for a late crew member";
    /** The minutes of a delay when the scenario does not name them. */
    private static final int ANY_MINUTES = 15;
    private static final String TOO_LONG = "at most 12 hours";
    private static final String INVALID_REASON = "Delay reason '50' is not in the IATA list";
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

    /** A colleague entered the delay, so "I last changed the delay" really tests the change. */
    @Given("flight {word} with STD {time}, STA {time} UTC and one departure delay of {int} minutes")
    public void flightWithOneDepartureDelay(String flightNumber, LocalTime std, LocalTime sta, int minutes) {
        world.plan(flightNumber, onDepartureDate(std), onDepartureDate(sta));
        Flight flight = world.flight(flightNumber);
        flight.addDelay(DelayKind.DEPARTURE, new DelayDuration(minutes), new DelayCode(VALID_REASON), DESCRIPTION,
            colleague, NOW, world.delayCodes);
        delayId = flight.getDelays().getLast().getId();
        estimatedBefore = flight.getEstimated();
    }

    @Given("{word} has a departure delay of {int} minutes")
    @Given("{word} already has a departure delay of {int} minutes")
    public void hasADepartureDelay(String flightNumber, int minutes) {
        addDelay(flightNumber, DelayKind.DEPARTURE, minutes);
    }

    @Given("{word} also has a(n) {delayKind} delay of {int} minutes")
    public void alsoHasADelay(String flightNumber, DelayKind kind, int minutes) {
        DelayId id = addDelay(flightNumber, kind, minutes);
        if (kind == DelayKind.ARRIVAL) {
            arrivalDelayId = id;
        }
    }

    @When("I enter a departure delay of {int} minutes with a valid delay reason and a description")
    public void iEnterAValidDepartureDelay(int minutes) {
        enterDelay(HJ221, DelayKind.DEPARTURE, minutes, VALID_REASON, DESCRIPTION);
    }

    @When("I enter a(n) {delayKind} delay of {int} minutes")
    public void iEnterADelay(DelayKind kind, int minutes) {
        world.expectedRefusal = TOO_LONG;
        enterDelay(HJ221, kind, minutes, VALID_REASON, DESCRIPTION);
    }

    @When("I enter another departure delay of {int} minutes and an arrival delay of {int} minutes")
    public void iEnterAnotherDepartureDelayAndAnArrivalDelay(int departureMinutes, int arrivalMinutes) {
        addDelay(HJ221, DelayKind.DEPARTURE, departureMinutes);
        addDelay(HJ221, DelayKind.ARRIVAL, arrivalMinutes);
    }

    @When("I enter a departure delay with a delay reason that is not in the IATA list")
    public void iEnterADepartureDelayWithAnUnknownReason() {
        world.expectedRefusal = INVALID_REASON;
        enterDelay(HJ221, DelayKind.DEPARTURE, ANY_MINUTES, UNKNOWN_REASON, DESCRIPTION);
    }

    @When("I enter a departure delay without a description")
    public void iEnterADepartureDelayWithoutADescription() {
        world.expectedRefusal = NO_DESCRIPTION;
        enterDelay(HJ221, DelayKind.DEPARTURE, ANY_MINUTES, VALID_REASON, "");
    }

    @When("I enter a departure delay of {int} minutes for {word}")
    public void iEnterADepartureDelayFor(int minutes, String flightNumber) {
        enterDelay(flightNumber, DelayKind.DEPARTURE, minutes, VALID_REASON, DESCRIPTION);
    }

    @When("I enter a departure delay")
    public void iEnterADepartureDelay() {
        world.expectedRefusal = NOT_PLANNED;
        enterDelay(HJ221, DelayKind.DEPARTURE, ANY_MINUTES, VALID_REASON, DESCRIPTION);
    }

    @When("I enter an arrival delay")
    public void iEnterAnArrivalDelay() {
        world.expectedRefusal = LANDED;
        enterDelay(HJ221, DelayKind.ARRIVAL, ANY_MINUTES, VALID_REASON, DESCRIPTION);
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

    @Then("the ETD is {time}")
    public void theEtdIs(LocalTime time) {
        assertEtd(time);
    }

    @Then("the ETD remains {time}")
    public void theEtdRemains(LocalTime time) {
        world.assertAccepted();
        assertEtd(time);
    }

    @And("the ETA is {time}")
    public void theEtaIs(LocalTime time) {
        Flight flight = world.flight(HJ221);
        assertThat(flight.getEstimated().arrival()).isEqualTo(onDate(flight.getScheduled().departureDate(), time));
    }

    @And("it is recorded that I entered the delay")
    public void itIsRecordedThatIEnteredTheDelay() {
        assertThat(world.flight(HJ221).getDelays().getLast().getLastChangedBy()).isEqualTo(world.planner);
    }

    @And("it is recorded that I last changed the delay")
    public void itIsRecordedThatILastChangedTheDelay() {
        world.assertAccepted();
        assertThat(delay(delayId).getLastChangedBy()).isEqualTo(world.planner);
    }

    @Then("the delay has the new description")
    public void theDelayHasTheNewDescription() {
        world.assertAccepted();
        assertThat(delay(delayId).getDescription()).isEqualTo(NEW_DESCRIPTION);
    }

    /** The domain has no way to change the reason (Delay.reason is final): a change keeps it. */
    @Then("the delay keeps its delay reason")
    public void theDelayKeepsItsDelayReason() {
        world.assertAccepted();
        assertThat(delay(delayId).getReason()).isEqualTo(new DelayCode(VALID_REASON));
    }

    @And("only the delay of {int} minutes remains")
    public void onlyTheDelayOfRemains(int minutes) {
        world.assertAccepted();
        assertThat(world.flight(HJ221).getDelays()).singleElement()
            .extracting(delay -> delay.getMinutes().minutes()).isEqualTo(minutes);
    }

    @And("the ETD and the ETA remain unchanged")
    public void theEtdAndTheEtaRemainUnchanged() {
        assertThat(world.flight(HJ221).getEstimated()).isEqualTo(estimatedBefore);
    }

    @Then("the ETD equals the STD again")
    public void theEtdEqualsTheStdAgain() {
        world.assertAccepted();
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

    @Then("the ETD of {word} is {date} {time} UTC")
    public void theEtdOfFlightIs(String flightNumber, LocalDate date, LocalTime time) {
        world.assertAccepted();
        assertThat(world.flight(flightNumber).getEstimated().departure()).isEqualTo(onDate(date, time));
    }

    @But("it remains the flight {word} of {date}")
    public void itRemainsTheFlight(String flightNumber, LocalDate date) {
        Flight flight = world.flight(flightNumber);
        assertThat(flight.getFlightNumber()).isEqualTo(new FlightNumber(flightNumber));
        assertThat(flight.getScheduled().departureDate()).isEqualTo(date);
    }

    private void assertEtd(LocalTime time) {
        Flight flight = world.flight(HJ221);
        assertThat(flight.getEstimated().departure()).isEqualTo(onDate(flight.getScheduled().departureDate(), time));
    }

    private DelayId addDelay(String flightNumber, DelayKind kind, int minutes) {
        Flight flight = world.flight(flightNumber);
        flight.addDelay(kind, new DelayDuration(minutes), new DelayCode(VALID_REASON), DESCRIPTION, world.planner,
            NOW, world.delayCodes);
        return flight.getDelays().getLast().getId();
    }

    private void enterDelay(String flightNumber, DelayKind kind, int minutes, String reason, String description) {
        world.attempt(() -> world.flight(flightNumber).addDelay(kind, new DelayDuration(minutes),
            new DelayCode(reason), description, world.planner, NOW, world.delayCodes));
    }

    private void changeDelay(DelayId id, int minutes, String description) {
        world.attempt(() -> events = world.flight(HJ221).changeDelay(id, new DelayDuration(minutes), description,
            world.planner, NOW));
    }

    private void removeDelay(DelayId id) {
        world.attempt(() -> events = world.flight(HJ221).removeDelay(id, world.planner, NOW));
    }

    /** "change" adds 5 minutes, which stays within all limits of these scenarios. */
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
}
