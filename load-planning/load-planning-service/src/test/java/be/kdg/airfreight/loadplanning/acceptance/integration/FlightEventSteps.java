package be.kdg.airfreight.loadplanning.acceptance.integration;

import be.kdg.airfreight.loadplanning.domain.flightcargo.AirportCode;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightId;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightNumber;
import io.cucumber.java.en.Given;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** The flight events that Messaging passes on to Load Planning. */
public class FlightEventSteps {
    private static final String DEPARTED = "the flight has departed";

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
}
