package be.kdg.airfreight.loadplanning.acceptance.integration;

import be.kdg.airfreight.loadplanning.acceptance.common.Weights;
import be.kdg.airfreight.loadplanning.domain.flightcargo.AirportCode;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightId;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightNumber;
import be.kdg.airfreight.loadplanning.domain.flightcargo.UldCode;
import be.kdg.airfreight.loadplanning.domain.flightcargo.Weight;
import ch.qos.logback.classic.Level;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.time.LocalDate;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/** The ULD bookings that Messaging passes on to Load Planning. */
public class UldBookingSteps {
    /** The ULD of a message when the scenario does not name it. */
    private static final UldCode ANY_ULD = new UldCode("AKE20417HJ");
    private static final Weight ANY_WEIGHT = Weights.of("1000 kg");

    private final MessagingEvents messaging;
    private final MessageWorld world;
    private final LogCapture logs;

    public UldBookingSteps(MessagingEvents messaging, MessageWorld world, LogCapture logs) {
        this.messaging = messaging;
        this.world = world;
        this.logs = logs;
    }

    @Given("{uld} is booked with {weight} on {word} of {date}")
    public void isBookedWith(UldCode code, String weight, String flightNumber, LocalDate date) {
        world.assertFlight(flightNumber, date);
        messaging.uldBookingReceived(world.getFlight(), code, Weights.of(weight));
        messaging.waitUntilHandled();
        assertThat(world.cargo().hasBooked(code)).as("%s is booked", code.value()).isTrue();
    }

    @When("a new ULD message, with a different identification, books {uld} with {weight} on the same flight")
    public void aNewUldMessageBooks(UldCode code, String weight) {
        world.setMessageId(messaging.uldBookingReceived(world.getFlight(), code, Weights.of(weight)));
        messaging.waitUntilHandled();
    }

    /** Messaging has already turned day 09 into a date, in the month of the flight of the scenario. */
    @When("a ULD message arrives for {word}, day {day}, {word}, and that flight does not exist")
    public void aUldMessageArrivesForAFlightThatDoesNotExist(String flightNumber, int day, String departureAirport) {
        FlightId unknown = new FlightId(new FlightNumber(flightNumber),
            world.getFlight().scheduledDepartureDate().withDayOfMonth(day), new AirportCode(departureAirport));
        assertThat(world.exists(unknown)).as("%s exists", unknown).isFalse();
        world.setExpectedReason("No cargo for flight " + flightNumber);
        world.setMessageId(messaging.uldBookingReceived(unknown, ANY_ULD, ANY_WEIGHT));
        messaging.waitUntilHandled();
    }

    @When("a ULD message for that flight arrives")
    public void aUldMessageForThatFlightArrives() {
        world.setMessageId(messaging.uldBookingReceived(world.getFlight(), ANY_ULD, ANY_WEIGHT));
        messaging.waitUntilHandled();
    }

    /** Story #29: a message without a field it needs, or of a version it does not know, is refused at once. */
    @When("a ULD message for that flight arrives with {}")
    public void aUldMessageForThatFlightArrivesWith(String problem) {
        Consumer<Map<String, Object>> change = switch (problem) {
            case "no ULD code" -> {
                world.setExpectedReason("Field 'uldCode' is missing");
                yield body -> body.remove("uldCode");
            }
            case "no version" -> {
                world.setExpectedReason("Field 'version' is missing");
                yield body -> body.remove("version");
            }
            case "version 2" -> {
                world.setExpectedReason("Version 2 of 'messaging.uld-booking.received' is unknown: expected version 1");
                yield body -> body.put("version", 2);
            }
            default -> throw new IllegalArgumentException("Unknown problem: " + problem);
        };
        world.setMessageId(messaging.uldBookingReceived(world.getFlight(), ANY_ULD, ANY_WEIGHT, change));
        messaging.waitUntilHandled();
    }

    @Then("there is still one booking of {uld} with {weight}")
    public void thereIsStillOneBookingOf(UldCode code, String weight) {
        Weight expected = Weights.of(weight);
        assertThat(world.cargo().getUlds())
            .filteredOn(uld -> uld.getCode().equals(code))
            .singleElement()
            .satisfies(uld -> {
                assertThat(uld.getWeight().value()).isEqualByComparingTo(expected.value());
                assertThat(uld.getWeight().unit()).isEqualTo(expected.unit());
            });
    }

    @Then("the second booking is logged")
    public void theSecondBookingIsLogged() {
        logs.assertLogged(Level.WARN, "is already booked", "booking ignored");
    }

    @Then("the booking is not applied")
    public void theBookingIsNotApplied() {
        assertThat(world.cargo().hasBooked(ANY_ULD)).as("%s is booked", ANY_ULD.value()).isFalse();
    }
}
