package be.kdg.airfreight.messaging.acceptance.integration;

import be.kdg.airfreight.messaging.acceptance.integration.MessagingTraffic.Stream;
import be.kdg.airfreight.messaging.domain.MessageType;
import be.kdg.airfreight.messaging.domain.outgoingmessage.OutgoingMessage;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Story #27: every partner message can be recognized. The system creates one from the event it comes from;
 * what it wrote is in the database, what the partners get is read from the partner queue of RabbitMQ.
 */
public class PartnerMessageSteps {
    private static final int FORMAT_VERSION = 1;
    private static final String VERSION_HEADER = "version";

    private final MessagingTraffic traffic;

    private List<OutgoingMessage> before;
    private List<OutgoingMessage> created;

    public PartnerMessageSteps(MessagingTraffic traffic) {
        this.traffic = traffic;
    }

    @When("the system creates a(n) {word}")
    public void theSystemCreates(String message) {
        switch (message) {
            case "FLM" -> receive(Stream.FLIGHTS, Messages.flightAnnounced(Messages.FLIGHT_NUMBER, Messages.DATE,
                "BRU", "ATH", Messages.STD.toString(), Messages.STA.toString()));
            case "MVT-ED" -> receive(Stream.FLIGHTS, flightEvent("departure-estimate-changed", "etd", "06:45"));
            case "MVT-EA" -> receive(Stream.FLIGHTS, flightEvent("arrival-estimate-changed", "eta", "10:00"));
            case "MVT-AD" -> receive(Stream.FLIGHTS, flightEvent("departed", "atd", "06:35"));
            case "MVT-AA" -> receive(Stream.FLIGHTS, flightEvent("landed", "ata", "09:50"));
            case "CPM" -> receive(Stream.LOADS, Messages.loadFinalized());
            default -> throw new IllegalArgumentException("Unknown message: " + message);
        }
        assertThat(created).extracting(OutgoingMessage::getType).containsExactly(typeOf(message));
    }

    /** Flight Operations sends both estimates when a departure delay is entered. */
    @When("one departure delay produces an MVT-ED and an MVT-EA")
    public void oneDepartureDelayProducesAnMvtEdAndAnMvtEa() {
        receive(Stream.FLIGHTS, flightEvent("departure-estimate-changed", "etd", "06:45"),
            flightEvent("arrival-estimate-changed", "eta", "10:00"));
        assertThat(created).extracting(OutgoingMessage::getType)
            .containsExactlyInAnyOrder(MessageType.MVT_ED, MessageType.MVT_EA);
    }

    @Given("an MVT-ED was published, but Messaging stopped before it marked the message as sent")
    public void anMvtEdWasPublishedButNotMarkedAsSent() {
        theSystemCreates("MVT-ED");
        traffic.nextPartnerMessage(theMessage().getId().id());
        traffic.forgetThatItWasSent(theMessage().getId().id());
    }

    /** The relay of the partner outbox runs every second: it finds the message unsent and sends it. */
    @When("Messaging starts again")
    public void messagingStartsAgain() {
        // nothing to do: the next run of the relay is the restart
    }

    @Then("that message has a unique identification, its creation time in UTC and the format version")
    public void thatMessageCanBeRecognized() {
        OutgoingMessage message = theMessage();
        assertThat(traffic.partnerMessages()).filteredOn(other -> other.getId().equals(message.getId())).hasSize(1);
        assertThat(message.getCreatedAt()).isEqualTo(ScenarioTime.NOW);
        assertThat(message.getFormatVersion().number()).isEqualTo(FORMAT_VERSION);
        if (message.getType() == MessageType.FLM || message.getType() == MessageType.CPM) {
            Map<String, Object> body = traffic.readJson(message.getBody());
            assertThat(body).containsEntry("messageId", message.getId().id().toString())
                .containsEntry("producedAt", ScenarioTime.NOW.toString())
                .containsEntry("version", FORMAT_VERSION)
                .containsEntry("type", message.getType().name());
        }
    }

    @Then("the body of the message contains only the three lines of the telegram")
    public void theBodyContainsOnlyTheTelegram() {
        assertThat(theMessage().getBody().lines()).hasSize(3);
    }

    @Then("the identification, creation time and format version are in the AMQP properties and headers")
    public void theyAreInTheAmqpPropertiesAndHeaders() {
        OutgoingMessage message = theMessage();
        MessageProperties properties = traffic.nextPartnerMessage(message.getId().id()).getMessageProperties();
        assertThat(properties.getMessageId()).isEqualTo(message.getId().id().toString());
        assertThat(properties.getTimestamp()).isEqualTo(Date.from(message.getCreatedAt()));
        assertThat(properties.<Integer>getHeader(VERSION_HEADER)).isEqualTo(FORMAT_VERSION);
        assertThat(properties.<String>getHeader("message-id")).isEqualTo(message.getId().id().toString());
        assertThat(properties.<String>getHeader("produced-at")).isEqualTo(message.getCreatedAt().toString());
    }

    @Then("their identifications differ")
    public void theirIdentificationsDiffer() {
        assertThat(created).extracting(OutgoingMessage::getId).doesNotHaveDuplicates();
    }

    @Then("the MVT-ED is sent again with the same identification")
    public void theMvtEdIsSentAgainWithTheSameIdentification() {
        Message again = traffic.nextPartnerMessage(theMessage().getId().id());
        assertThat(again.getMessageProperties().getMessageId()).isEqualTo(theMessage().getId().id().toString());
        assertThat(again.getBody()).isEqualTo(theMessage().getBody().getBytes(StandardCharsets.UTF_8));
    }

    @SafeVarargs
    private void receive(Stream stream, Map<String, Object>... messages) {
        traffic.waitUntilHandled(stream);
        before = traffic.partnerMessages();
        for (Map<String, Object> message : messages) {
            traffic.send(stream, message);
            traffic.awaitReceived(Messages.idOf(message));
        }
        created = traffic.partnerMessages().stream().filter(message -> !before.contains(message)).toList();
    }

    private OutgoingMessage theMessage() {
        assertThat(created).hasSize(1);
        return created.getFirst();
    }

    /** A flight event of HJ221 of 9 January with one time of that day. */
    private static Map<String, Object> flightEvent(String name, String field, String time) {
        Map<String, Object> event = Messages.flightEvent(name);
        event.put(field, Messages.DATE + "T" + time + ":00Z");
        return event;
    }

    private static MessageType typeOf(String message) {
        return MessageType.valueOf(message.replace('-', '_'));
    }
}
