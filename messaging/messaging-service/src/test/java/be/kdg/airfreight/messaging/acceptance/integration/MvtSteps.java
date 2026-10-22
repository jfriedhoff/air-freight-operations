package be.kdg.airfreight.messaging.acceptance.integration;

import be.kdg.airfreight.messaging.acceptance.integration.MessagingTraffic.Stream;
import be.kdg.airfreight.messaging.domain.outgoingmessage.OutgoingMessage;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Stories 11 and 14. "Flight Operations reports ..." publishes the flight events on the exchange of Flight
 * Operations, as Flight Operations does. "This telegram is sent" finds the telegram among the partner messages
 * that Messaging wrote and waits until the relay has sent it. The @unit scenarios check the telegram after
 * midnight; these scenarios check that an event on the queue ends up at the partners.
 */
public class MvtSteps {
    private final MessagingTraffic traffic;

    private List<OutgoingMessage> before;

    public MvtSteps(MessagingTraffic traffic) {
        this.traffic = traffic;
    }

    @When("Flight Operations reports an ETD of {moment} and an ETA of {moment} UTC")
    public void flightOperationsReportsNewEstimates(Instant etd, Instant eta) {
        report(flightEvent("departure-estimate-changed", "etd", etd),
            flightEvent("arrival-estimate-changed", "eta", eta));
    }

    @When("Flight Operations reports an ATD of {moment} and an ATA of {moment} UTC")
    public void flightOperationsReportsActualTimes(Instant atd, Instant ata) {
        report(flightEvent("departed", "atd", atd), flightEvent("landed", "ata", ata));
    }

    @Then("this telegram is sent:")
    public void thisTelegramIsSent(String telegram) {
        List<OutgoingMessage> written = traffic.partnerMessages().stream()
            .filter(message -> !before.contains(message))
            .toList();
        assertThat(written).extracting(OutgoingMessage::getBody).contains(telegram);
        OutgoingMessage message = written.stream()
            .filter(candidate -> candidate.getBody().equals(telegram))
            .findFirst()
            .orElseThrow();
        traffic.awaitSent(message.getId().id());
    }

    @SafeVarargs
    private void report(Map<String, Object>... events) {
        traffic.waitUntilHandled(Stream.FLIGHTS);
        before = traffic.partnerMessages();
        for (Map<String, Object> event : events) {
            traffic.send(Stream.FLIGHTS, event);
            traffic.awaitReceived(Messages.idOf(event));
        }
    }

    private static Map<String, Object> flightEvent(String name, String field, Instant time) {
        Map<String, Object> event = Messages.flightEvent(name);
        event.put(field, time.toString());
        return event;
    }
}
