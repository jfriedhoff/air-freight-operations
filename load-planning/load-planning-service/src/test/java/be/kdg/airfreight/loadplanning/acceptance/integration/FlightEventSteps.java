package be.kdg.airfreight.loadplanning.acceptance.integration;

import be.kdg.airfreight.loadplanning.TestClock;
import be.kdg.airfreight.loadplanning.domain.flightcargo.AirportCode;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargo;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightId;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightNumber;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/** The flight events that Messaging passes on to Load Planning. */
public class FlightEventSteps {
    private static final String DEPARTED = "the flight has departed";
    /** The flight of a scenario that names no date: HJ221 of 9 January 2026 from BRU, as in the other stories. */
    private static final LocalDate DEPARTURE_DATE = LocalDate.of(2026, 1, 9);
    private static final AirportCode BRU = new AirportCode("BRU");
    /** A delay of 25 minutes on the STD of 06:45 UTC that MessagingEvents announces every flight with. */
    private static final LocalTime DELAYED_ETD = LocalTime.of(7, 10);
    /**
     * With an STD of 06:45 the loading period runs from 18:45 the day before until 06:25. At 05:00 it is
     * open, so the delay moves its end to 06:50, and a duplicate could move it again.
     */
    private static final LocalTime IN_THE_LOADING_PERIOD = LocalTime.of(5, 0);
    private static final LocalTime LOADING_CLOSES_AFTER_DELAY = LocalTime.of(6, 50);

    private final MessagingEvents messaging;
    private final MessageWorld world;
    private final TestClock clock;
    /** The flight as Load Planning stored it after the first event, to compare with after the duplicate. */
    private FlightCargo afterFirstEvent;

    public FlightEventSteps(MessagingEvents messaging, MessageWorld world, TestClock clock) {
        this.messaging = messaging;
        this.world = world;
        this.clock = clock;
    }

    @After
    public void resetTheClock() {
        clock.reset();
    }

    @Given("flight {word} of {date} from {word} exists")
    public void flightExists(String flightNumber, LocalDate date, String departureAirport) {
        world.setFlight(new FlightId(new FlightNumber(flightNumber), date, new AirportCode(departureAirport)));
        messaging.flightAnnounced(world.getFlight());
        messaging.waitUntilHandled();
        assertThat(world.cargo().getUlds()).isEmpty();
    }

    /** A booking for this flight must then be set aside for that reason. */
    @Given("flight {word} of {date} has departed")
    public void flightHasDeparted(String flightNumber, LocalDate date) {
        world.assertFlight(flightNumber, date);
        messaging.flightDeparted(world.getFlight());
        messaging.waitUntilHandled();
        assertThat(world.cargo().getFlightInfo().departed()).isTrue();
        world.setExpectedReason(DEPARTED);
    }

    @Given("Flight Operations sends an ETD of {time} UTC and then an ETD of {time} UTC for {word}")
    public void flightOperationsSendsTwoEtds(LocalTime first, LocalTime second, String flightNumber) {
        world.setFlight(new FlightId(new FlightNumber(flightNumber), DEPARTURE_DATE, BRU));
        messaging.flightAnnounced(world.getFlight());
        messaging.departureEstimateChanged(world.getFlight(), onFlightDate(first));
        messaging.departureEstimateChanged(world.getFlight(), onFlightDate(second));
    }

    @When("Load Planning processes both events")
    public void loadPlanningProcessesBothEvents() {
        messaging.waitUntilHandled();
    }

    @Then("the ETD in Load Planning is {time} UTC")
    public void theEtdInLoadPlanningIs(LocalTime etd) {
        assertThat(world.cargo().getFlightInfo().etd()).isEqualTo(onFlightDate(etd));
    }

    /** The loading period is still open, so the event changes both the ETD and the end of the loading period. */
    @Given("Load Planning has already processed the event about the delay of {word}")
    public void loadPlanningHasAlreadyProcessedTheDelayOf(String flightNumber) {
        world.setFlight(new FlightId(new FlightNumber(flightNumber), DEPARTURE_DATE, BRU));
        clock.set(onFlightDate(IN_THE_LOADING_PERIOD));
        messaging.flightAnnounced(world.getFlight());
        world.setMessageId(messaging.departureEstimateChanged(world.getFlight(), onFlightDate(DELAYED_ETD)));
        messaging.waitUntilHandled();
        afterFirstEvent = world.cargo();
        assertThat(afterFirstEvent.getFlightInfo().etd()).isEqualTo(onFlightDate(DELAYED_ETD));
        assertThat(afterFirstEvent.getLoadingClosesAt()).isEqualTo(onFlightDate(LOADING_CLOSES_AFTER_DELAY));
    }

    @When("the same event arrives again")
    public void theSameEventArrivesAgain() {
        messaging.sendAgain(world.getMessageId());
        messaging.waitUntilHandled();
    }

    /**
     * Compares every field except the version. Load Planning has no inbox, so it handles the duplicate like
     * any other event and saves the flight again, and DbFlightCargoRepository raises the version on every
     * save. The version is not part of the API, so no planner and no other context can see it change.
     */
    @Then("nothing changes")
    public void nothingChanges() {
        assertThat(world.cargo()).usingRecursiveComparison().ignoringFields("version").isEqualTo(afterFirstEvent);
    }

    private Instant onFlightDate(LocalTime time) {
        return world.getFlight().scheduledDepartureDate().atTime(time).toInstant(ZoneOffset.UTC);
    }
}
