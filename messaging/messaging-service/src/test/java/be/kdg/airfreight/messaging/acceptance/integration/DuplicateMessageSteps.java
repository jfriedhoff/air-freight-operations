package be.kdg.airfreight.messaging.acceptance.integration;

import be.kdg.airfreight.messaging.acceptance.integration.MessagingTraffic.Stream;
import be.kdg.airfreight.messaging.domain.MessageType;
import be.kdg.airfreight.messaging.domain.outgoingmessage.OutgoingMessage;
import be.kdg.airfreight.messaging.domain.receivedmessage.ReceivedMessageStatus;
import ch.qos.logback.classic.Level;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Stories #29 and #28: the inbox of Messaging recognizes a message it has seen before by its id. */
public class DuplicateMessageSteps {
    private static final String ULD_BOOKING_RECEIVED = "messaging.uld-booking.received";

    private final MessagingTraffic traffic;
    private final LogCapture logs;

    private Stream stream;
    private final List<Map<String, Object>> sent = new ArrayList<>();
    /** What Messaging had passed on and written before the scenario: older messages in a dev queue. */
    private int bookingsBefore;
    private List<OutgoingMessage> partnerMessagesBefore;

    public DuplicateMessageSteps(MessagingTraffic traffic, LogCapture logs) {
        this.traffic = traffic;
        this.logs = logs;
    }

    @Given("a ULD message has been processed")
    public void aUldMessageHasBeenProcessed() {
        start(Stream.BOOKINGS);
        process(Messages.uldMessage());
        assertThat(traffic.passedOn(ULD_BOOKING_RECEIVED)).hasSize(bookingsBefore + 1);
    }

    @When("the same message, with the same identification, arrives again")
    public void theSameMessageArrivesAgain() {
        sent.forEach(message -> traffic.send(stream, message));
        traffic.waitUntilHandled(stream);
    }

    @Then("nothing changes")
    public void nothingChanges() {
        assertThat(traffic.passedOn(ULD_BOOKING_RECEIVED)).hasSize(bookingsBefore + 1);
        assertThat(traffic.receivedMessage(Messages.idOf(sent.getFirst())).orElseThrow().getStatus())
            .isEqualTo(ReceivedMessageStatus.PROCESSED);
    }

    /** A departure delay gives Messaging two events: the new ETD and the new ETA. Both arrive twice. */
    @When("Messaging receives the same event about the departure delay of HJ221 twice")
    public void messagingReceivesTheSameEventTwice() {
        start(Stream.FLIGHTS);
        Map<String, Object> etd = Messages.flightEvent("departure-estimate-changed");
        etd.put("etd", "2026-01-09T06:45:00Z");
        Map<String, Object> eta = Messages.flightEvent("arrival-estimate-changed");
        eta.put("eta", "2026-01-09T10:00:00Z");
        process(etd);
        process(eta);
        theSameMessageArrivesAgain();
    }

    @Then("one MVT-ED and one MVT-EA are created")
    public void oneMvtEdAndOneMvtEaAreCreated() {
        assertThat(traffic.partnerMessages())
            .filteredOn(message -> !partnerMessagesBefore.contains(message))
            .extracting(OutgoingMessage::getType)
            .containsExactlyInAnyOrder(MessageType.MVT_ED, MessageType.MVT_EA);
    }

    @Then("the duplicate is logged")
    @Then("the duplicate is logged with its identification")
    public void theDuplicateIsLogged() {
        for (Map<String, Object> message : sent) {
            logs.assertLogged(Level.INFO, "Duplicate", Messages.idOf(message).toString(), "ignored");
        }
    }

    private void start(Stream stream) {
        this.stream = stream;
        traffic.waitUntilHandled(stream);
        bookingsBefore = traffic.passedOn(ULD_BOOKING_RECEIVED).size();
        partnerMessagesBefore = traffic.partnerMessages();
    }

    private void process(Map<String, Object> message) {
        UUID messageId = Messages.idOf(message);
        traffic.send(stream, message);
        traffic.awaitReceived(messageId);
        sent.add(message);
    }
}
