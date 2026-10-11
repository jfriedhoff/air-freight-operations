package be.kdg.airfreight.loadplanning.acceptance.integration;

import be.kdg.airfreight.loadplanning.domain.flightcargo.AirportCode;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightId;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightNumber;
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

    private final MessagingEvents messaging;
    private final MessageWorld world;

    public FlightEventSteps(MessagingEvents messaging, MessageWorld world) {
        this.messaging = messaging;
        this.world = world;
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

    private Instant onFlightDate(LocalTime time) {
        return world.getFlight().scheduledDepartureDate().atTime(time).toInstant(ZoneOffset.UTC);
    }
}
