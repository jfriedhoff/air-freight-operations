package be.kdg.airfreight.messaging.acceptance.integration;

import be.kdg.airfreight.messaging.acceptance.integration.MessagingTraffic.Stream;
import be.kdg.airfreight.messaging.domain.MessageType;
import be.kdg.airfreight.messaging.domain.outgoingmessage.OutgoingMessage;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Story 25. "Load Planning reports the finalized load" publishes a load-finalized event on the exchange of Load
 * Planning, as Load Planning does. The CPM is read from the partner messages that Messaging wrote, and the message
 * that tells Flight Operations the CPM is written is read from the outbox of Messaging.
 */
public class CpmSteps {
    private static final String CPM_CREATED = "messaging.cpm.created";

    private final MessagingTraffic traffic;

    private List<OutgoingMessage> before;
    private int cpmCreatedBefore;

    public CpmSteps(MessagingTraffic traffic) {
        this.traffic = traffic;
    }

    @When("Load Planning reports the finalized load of {word} with {word} on {word}, {int} kg, destination {word}")
    public void theFinalizedLoadWithOneUld(String flightNumber, String uld, String position, int kilograms,
                                           String destination) {
        assertThat(flightNumber).isEqualTo(Messages.FLIGHT_NUMBER);
        Map<String, Object> event = Messages.loadFinalized();
        event.put("load", List.of(Map.of("uldCode", uld, "position", position, "weight", kilograms,
            "weightUnit", "KG", "destination", destination)));
        report(event);
    }

    @When("Load Planning reports the finalized load of {word} without ULDs")
    public void theFinalizedLoadWithoutUlds(String flightNumber) {
        assertThat(flightNumber).isEqualTo(Messages.FLIGHT_NUMBER);
        Map<String, Object> event = Messages.loadFinalized();
        event.put("load", List.of());
        report(event);
    }

    @Then("a CPM is sent with flight number {word}, scheduled departure date {date} and departure airport {word}")
    public void aCpmIsSentWith(String flightNumber, LocalDate date, String departureAirport) {
        assertThat(theCpm())
            .containsEntry("flightNumber", flightNumber)
            .containsEntry("scheduledDepartureDate", date.toString())
            .containsEntry("departureAirport", departureAirport);
    }

    @Then("the CPM contains {word} with {word}, {int} kg and {word}")
    public void theCpmContains(String uld, String position, int kilograms, String destination) {
        assertThat(ulds()).singleElement().satisfies(line -> {
            assertThat(line)
                .containsEntry("uldCode", uld)
                .containsEntry("position", position)
                .containsEntry("weightUnit", "KG")
                .containsEntry("destination", destination);
            assertThat(((Number) line.get("weight")).intValue()).isEqualTo(kilograms);
        });
    }

    @Then("a CPM is sent with an empty list of ULDs")
    public void aCpmIsSentWithAnEmptyListOfUlds() {
        assertThat(ulds()).isEmpty();
    }

    @Then("Flight Operations is told that the CPM of {word} of {date} from {word} has been written")
    public void flightOperationsIsToldThatTheCpmHasBeenWritten(String flightNumber, LocalDate date,
                                                               String departureAirport) {
        assertThat(traffic.passedOn(CPM_CREATED))
            .hasSize(cpmCreatedBefore + 1)
            .anySatisfy(event -> assertThat(event)
                .containsEntry("flightNumber", flightNumber)
                .containsEntry("scheduledDepartureDate", date.toString())
                .containsEntry("departureAirport", departureAirport));
    }

    private void report(Map<String, Object> event) {
        traffic.waitUntilHandled(Stream.LOADS);
        before = traffic.partnerMessages();
        cpmCreatedBefore = traffic.passedOn(CPM_CREATED).size();
        traffic.send(Stream.LOADS, event);
        traffic.awaitReceived(Messages.idOf(event));
    }

    /** The body of the one CPM this scenario produced, once the relay has sent it. */
    private Map<String, Object> theCpm() {
        List<OutgoingMessage> cpms = traffic.partnerMessages().stream()
            .filter(message -> !before.contains(message))
            .filter(message -> message.getType() == MessageType.CPM)
            .toList();
        assertThat(cpms).hasSize(1);
        traffic.awaitSent(cpms.getFirst().getId().id());
        return traffic.readJson(cpms.getFirst().getBody());
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> ulds() {
        return (List<Map<String, Object>>) theCpm().get("ulds");
    }
}
