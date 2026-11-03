package be.kdg.airfreight.messaging.acceptance.integration;

import be.kdg.airfreight.messaging.acceptance.integration.MessagingTraffic.Stream;
import be.kdg.airfreight.messaging.domain.AirportCode;
import be.kdg.airfreight.messaging.domain.FlightId;
import be.kdg.airfreight.messaging.domain.FlightNumber;
import be.kdg.airfreight.messaging.domain.MessageId;
import be.kdg.airfreight.messaging.domain.MessageType;
import be.kdg.airfreight.messaging.domain.outgoingmessage.OutgoingMessage;
import be.kdg.airfreight.messaging.domain.receivedmessage.ReceivedMessage;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Story 38 in Messaging: Flight Operations reports that it removed a flight, and Messaging deletes what it kept
 * about that flight, the received messages and the partner messages, in the same transaction. The messages of a
 * flight are published on the exchanges of their senders, as in the other scenarios, and read back from the
 * database: the point of the story is that those rows are gone. Cucumber makes a new instance for every scenario.
 */
public class FlightRemovalSteps {
    private static final String FLIGHT_REMOVED = "messaging.flight.removed";
    private static final String BRU = "BRU";

    private final MessagingTraffic traffic;

    /** The ids of the received and the partner messages of each flight, as they were before the removal. */
    private final Map<FlightId, List<MessageId>> receivedBefore = new HashMap<>();
    private final Map<FlightId, List<MessageId>> partnerMessagesBefore = new HashMap<>();
    private UUID removal;
    private int removalsPassedOnBefore;

    public FlightRemovalSteps(MessagingTraffic traffic) {
        this.traffic = traffic;
    }

    /**
     * Four received messages that name the flight, and three partner messages for it: the FLM, the MVT-ED and the
     * CPM. The steps wait until the relay has sent the partner messages, as it has long done when a flight is
     * removed a week after its date; the relay then no longer writes to the rows the removal deletes.
     */
    @Given("Messaging has handled the announcement, a delay, a ULD booking and the finalized load of {word} of {date}")
    public void messagingHasHandledTheMessagesOf(String flightNumber, LocalDate date) {
        FlightId flight = flight(flightNumber, date);
        // The id Flight Operations gives the flight: the same in every event about it.
        String reference = UUID.randomUUID().toString();
        Map<String, Object> announcement = Messages.flightAnnounced(flightNumber, date, BRU, "ATH",
            utc(date, 6, 30), utc(date, 9, 45));
        announcement.put("flightId", reference);
        process(Stream.FLIGHTS, announcement);
        Map<String, Object> delay = Messages.flightEvent("departure-estimate-changed");
        delay.put("flightId", reference);
        delay.put("flightNumber", flightNumber);
        delay.put("scheduledDepartureDate", date.toString());
        delay.put("etd", utc(date, 6, 45));
        process(Stream.FLIGHTS, delay);
        Map<String, Object> booking = Messages.uldMessage();
        booking.put("flightNumber", flightNumber);
        booking.put("dayOfMonth", date.getDayOfMonth());
        process(Stream.BOOKINGS, booking);
        Map<String, Object> load = Messages.loadFinalized();
        load.put("flightNumber", flightNumber);
        load.put("scheduledDepartureDate", date.toString());
        process(Stream.LOADS, load);

        List<ReceivedMessage> received = receivedAbout(flight);
        assertThat(received).extracting(ReceivedMessage::getType).containsExactlyInAnyOrder(MessageType.FLIGHT_EVENT,
            MessageType.FLIGHT_EVENT, MessageType.ULD, MessageType.LOAD_FINALIZED);
        List<OutgoingMessage> partnerMessages = partnerMessagesAbout(flight);
        assertThat(partnerMessages).extracting(OutgoingMessage::getType)
            .containsExactlyInAnyOrder(MessageType.FLM, MessageType.MVT_ED, MessageType.CPM);
        partnerMessages.forEach(message -> traffic.awaitSent(message.getId().id()));
        receivedBefore.put(flight, received.stream().map(ReceivedMessage::getId).toList());
        partnerMessagesBefore.put(flight, partnerMessages.stream().map(OutgoingMessage::getId).toList());
    }

    @When("Flight Operations reports that {word} of {date} has been removed")
    public void flightOperationsReportsThatTheFlightHasBeenRemoved(String flightNumber, LocalDate date) {
        removalsPassedOnBefore = traffic.passedOn(FLIGHT_REMOVED).size();
        Map<String, Object> event = Messages.flightEvent("removed");
        event.put("flightId", UUID.randomUUID().toString());
        event.put("flightNumber", flightNumber);
        event.put("scheduledDepartureDate", date.toString());
        removal = Messages.idOf(event);
        traffic.send(Stream.FLIGHTS, event);
        traffic.awaitReceived(removal);
    }

    /**
     * The received removal stays: the inbox needs it to recognize the same event when it arrives again. No partner
     * message is left either, so the removal itself wrote none.
     */
    @Then("Messaging keeps no message about {word} of {date} except the removal itself")
    public void messagingKeepsNoMessageAboutTheFlightExceptTheRemoval(String flightNumber, LocalDate date) {
        FlightId flight = flight(flightNumber, date);
        assertThat(receivedAbout(flight)).extracting(message -> message.getId().id()).containsExactly(removal);
        assertThat(partnerMessagesAbout(flight)).isEmpty();
    }

    @Then("the messages about {word} of {date} are all still there")
    public void theMessagesAboutTheOtherFlightAreStillThere(String flightNumber, LocalDate date) {
        FlightId flight = flight(flightNumber, date);
        assertThat(receivedAbout(flight)).extracting(ReceivedMessage::getId)
            .containsExactlyInAnyOrderElementsOf(receivedBefore.get(flight));
        assertThat(partnerMessagesAbout(flight)).extracting(OutgoingMessage::getId)
            .containsExactlyInAnyOrderElementsOf(partnerMessagesBefore.get(flight));
    }

    /** Load Planning deletes the cargo of the flight on this event, so it must be passed on. */
    @Then("Load Planning is told that {word} of {date} has been removed")
    public void loadPlanningIsToldThatTheFlightHasBeenRemoved(String flightNumber, LocalDate date) {
        assertThat(traffic.passedOn(FLIGHT_REMOVED))
            .hasSize(removalsPassedOnBefore + 1)
            .anySatisfy(event -> assertThat(event)
                .containsEntry("flightNumber", flightNumber)
                .containsEntry("scheduledDepartureDate", date.toString())
                .containsEntry("departureAirport", BRU));
    }

    private void process(Stream stream, Map<String, Object> message) {
        traffic.send(stream, message);
        traffic.awaitReceived(Messages.idOf(message));
    }

    private List<ReceivedMessage> receivedAbout(FlightId flight) {
        return traffic.receivedMessages().stream().filter(message -> flight.equals(message.getFlightId())).toList();
    }

    private List<OutgoingMessage> partnerMessagesAbout(FlightId flight) {
        return traffic.partnerMessages().stream().filter(message -> flight.equals(message.getFlightId())).toList();
    }

    private static FlightId flight(String flightNumber, LocalDate date) {
        return new FlightId(new FlightNumber(flightNumber), date, new AirportCode(BRU));
    }

    private static String utc(LocalDate date, int hour, int minute) {
        return date.atTime(hour, minute).toInstant(ZoneOffset.UTC).toString();
    }
}
