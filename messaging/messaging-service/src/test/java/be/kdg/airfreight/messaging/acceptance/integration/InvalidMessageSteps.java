package be.kdg.airfreight.messaging.acceptance.integration;

import be.kdg.airfreight.messaging.acceptance.integration.MessagingTraffic.Stream;
import be.kdg.airfreight.messaging.domain.receivedmessage.ReceivedMessage;
import be.kdg.airfreight.messaging.domain.receivedmessage.ReceivedMessageStatus;
import ch.qos.logback.classic.Level;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/** Story #29: an invalid incoming message is set aside in the inbox, with the reason, and logged. */
public class InvalidMessageSteps {
    private static final String UNREADABLE_JSON = "{\"messageId\": ";
    private static final String NOT_READABLE = "not readable JSON";
    private static final Pattern WEIGHT = Pattern.compile("weight (-?[\\d.]+) (\\w+)");
    private static final String ULD_BOOKING_RECEIVED = "messaging.uld-booking.received";
    private static final String FLIGHT_SCHEDULED = "messaging.flight.scheduled";
    private static final LocalDate MONDAY = LocalDate.of(2026, 1, 5);

    private final MessagingTraffic traffic;
    private final LogCapture logs;

    /** The id of the message, or null when the message is so broken that it has none. */
    private UUID messageId;
    /** What Messaging had passed on before the message arrived. */
    private int bookingsBefore;
    private int flightsBefore;

    public InvalidMessageSteps(MessagingTraffic traffic, LogCapture logs) {
        this.traffic = traffic;
        this.logs = logs;
    }

    @When("a ULD message arrives with {}")
    public void aUldMessageArrivesWith(String error) {
        if (error.equals("JSON that cannot be read")) {
            countWhatWasPassedOn(Stream.BOOKINGS);
            traffic.send(Stream.BOOKINGS, "cargo-booking.uld", UNREADABLE_JSON);
            traffic.waitUntilHandled(Stream.BOOKINGS);
            return;
        }
        Map<String, Object> message = Messages.uldMessage();
        Matcher weight = WEIGHT.matcher(error);
        if (weight.matches()) {
            message.put("weight", new BigDecimal(weight.group(1)));
            message.put("weightUnit", weight.group(2).toUpperCase());
        } else {
            switch (error) {
                case "no ULD code" -> message.remove("uldCode");
                case "ULD code AK16689R7" -> message.put("uldCode", "AK16689R7");
                case "a weight without a unit" -> message.remove("weightUnit");
                case "a destination that is not in the IATA list" -> message.put("destination", "QQQ");
                case "day 32" -> message.put("dayOfMonth", 32);
                default -> throw new IllegalArgumentException("Unknown error: " + error);
            }
        }
        arrives(Stream.BOOKINGS, message);
    }

    @When("an SSM arrives with {}")
    public void anSsmArrivesWith(String error) {
        Map<String, Object> message = Messages.ssm(MONDAY, MONDAY);
        switch (error) {
            case "an end date before the start date" -> message.put("periodTo", MONDAY.minusDays(1).toString());
            case "no weekdays" -> message.put("weekdays", List.of());
            case "an airport that is not in the IATA list" -> message.put("arrivalAirport", "QQQ");
            case "the same departure and arrival airport" -> message.put("arrivalAirport", "BRU");
            case "departure time 25:00" -> message.put("departureTime", "25:00");
            case "no aircraft registration" -> message.remove("registration");
            case "a flight number without a letter (221)" -> message.put("flightNumber", "221");
            default -> throw new IllegalArgumentException("Unknown error: " + error);
        }
        arrives(Stream.SCHEDULE, message);
    }

    @Then("it is not applied")
    public void itIsNotApplied() {
        assertThat(traffic.passedOn(ULD_BOOKING_RECEIVED)).as("bookings passed on to Load Planning")
            .hasSize(bookingsBefore);
    }

    @Then("no flights are created")
    public void noFlightsAreCreated() {
        assertThat(traffic.passedOn(FLIGHT_SCHEDULED)).as("flights passed on to Flight Operations")
            .hasSize(flightsBefore);
    }

    /** Unreadable JSON has no id of its own: Messaging sets it aside under a new one. */
    @Then("it is set aside and logged with its identification and the reason")
    public void itIsSetAsideAndLogged() {
        ReceivedMessage setAside = messageId != null
            ? traffic.receivedMessage(messageId).orElseThrow(() -> new AssertionError("Not in the inbox"))
            : traffic.receivedMessages().stream()
                .filter(message -> String.valueOf(message.getRejectionReason()).contains(NOT_READABLE))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No unreadable message in the inbox"));
        assertThat(setAside.getStatus()).isEqualTo(ReceivedMessageStatus.REJECTED);
        assertThat(setAside.getRejectionReason()).isNotBlank();
        logs.assertLogged(Level.WARN, setAside.getId().id().toString(), setAside.getRejectionReason());
    }

    private void arrives(Stream stream, Map<String, Object> message) {
        countWhatWasPassedOn(stream);
        messageId = Messages.idOf(message);
        traffic.send(stream, message);
        traffic.awaitReceived(messageId);
    }

    /**
     * A queue of the development environment can still hold older messages, which the listeners process
     * first: they are handled before the count, so only the message of the scenario can change it.
     */
    private void countWhatWasPassedOn(Stream stream) {
        traffic.waitUntilHandled(stream);
        bookingsBefore = traffic.passedOn(ULD_BOOKING_RECEIVED).size();
        flightsBefore = traffic.passedOn(FLIGHT_SCHEDULED).size();
    }
}
