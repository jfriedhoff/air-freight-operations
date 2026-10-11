package be.kdg.airfreight.messaging.acceptance.integration;

import be.kdg.airfreight.messaging.acceptance.integration.MessagingTraffic.Stream;
import be.kdg.airfreight.messaging.domain.MessageType;
import be.kdg.airfreight.messaging.domain.outgoingmessage.OutgoingMessage;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Story #3: an FLM for every new flight. Messaging takes the flights from the SSM and passes them on;
 * Flight Operations decides which are new and announces those (story #2). Here the test plays Flight
 * Operations through its JSON contract: it announces every flight that Messaging passed on.
 */
public class FlmSteps {
    private static final String FLIGHT_SCHEDULED = "messaging.flight.scheduled";
    private static final LocalDate MONDAY = LocalDate.of(2026, 1, 5);
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final MessagingTraffic traffic;

    private Map<String, Object> ssm;
    private List<Map<String, Object>> passedOnBefore;
    private List<OutgoingMessage> before;
    private List<Map<String, Object>> flights;

    public FlmSteps(MessagingTraffic traffic) {
        this.traffic = traffic;
    }

    /** HJ221 on the Mondays 5 and 12 January 2026. */
    @Given("an SSM that announces two new flights")
    public void anSsmThatAnnouncesTwoNewFlights() {
        ssm = Messages.ssm(MONDAY, MONDAY.plusWeeks(1));
    }

    @Given("an SSM for {word} from {word} to {word} on {weekday} {date} with departure {time} and arrival {time} UTC")
    public void anSsmFor(String flightNumber, String departureAirport, String arrivalAirport, DayOfWeek weekday,
                         LocalDate date, LocalTime departure, LocalTime arrival) {
        assertThat(date.getDayOfWeek()).isEqualTo(weekday);
        ssm = Messages.ssm(date, date);
        ssm.put("flightNumber", flightNumber);
        ssm.put("departureAirport", departureAirport);
        ssm.put("arrivalAirport", arrivalAirport);
        ssm.put("departureTime", departure.toString());
        ssm.put("arrivalTime", arrival.toString());
    }

    @When("the SSM is processed")
    public void theSsmIsProcessed() {
        traffic.waitUntilHandled(Stream.SCHEDULE);
        traffic.waitUntilHandled(Stream.FLIGHTS);
        passedOnBefore = traffic.passedOn(FLIGHT_SCHEDULED);
        before = traffic.partnerMessages();
        traffic.send(Stream.SCHEDULE, ssm);
        traffic.awaitReceived(Messages.idOf(ssm));
        flights = traffic.passedOn(FLIGHT_SCHEDULED).stream().filter(flight -> !passedOnBefore.contains(flight))
            .toList();
        for (Map<String, Object> flight : flights) {
            Map<String, Object> announced = flightOperationsAnnounces(flight);
            traffic.send(Stream.FLIGHTS, announced);
            traffic.awaitReceived(Messages.idOf(announced));
        }
    }

    @Then("two FLMs are sent")
    public void twoFlmsAreSent() {
        assertThat(flights).hasSize(2);
        await().atMost(TIMEOUT).untilAsserted(() -> assertThat(newFlms()).hasSize(2).allMatch(OutgoingMessage::isSent));
    }

    @Then("each FLM contains the flight number, the scheduled departure date, STD, STA and the departure and arrival airport")
    public void eachFlmContainsTheFlight() {
        List<Map<String, Object>> bodies = newFlms().stream().map(flm -> traffic.readJson(flm.getBody())).toList();
        for (Map<String, Object> flight : flights) {
            Instant std = Instant.parse(String.valueOf(flight.get("std")));
            assertThat(bodies).anySatisfy(body -> assertThat(body)
                .containsEntry("flightNumber", flight.get("flightNumber"))
                .containsEntry("scheduledDepartureDate", LocalDate.ofInstant(std, ZoneOffset.UTC).toString())
                .containsEntry("std", flight.get("std"))
                .containsEntry("sta", flight.get("sta"))
                .containsEntry("departureAirport", flight.get("departureAirport"))
                .containsEntry("arrivalAirport", flight.get("arrivalAirport")));
        }
    }

    @Then("STD and STA are full UTC timestamps in the FLM")
    public void stdAndStaAreFullUtcTimestamps() {
        for (OutgoingMessage flm : newFlms()) {
            Map<String, Object> body = traffic.readJson(flm.getBody());
            for (String field : List.of("std", "sta")) {
                assertThat(String.valueOf(body.get(field))).as(field)
                    .matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(:\\d{2})?Z");
            }
        }
    }

    @Then("the FLM of {word} contains the scheduled departure date {date}, the STD {moment} UTC and the STA {moment} UTC")
    public void theFlmContains(String flightNumber, LocalDate date, Instant std, Instant sta) {
        assertThat(newFlms()).singleElement().satisfies(flm -> assertThat(traffic.readJson(flm.getBody()))
            .containsEntry("flightNumber", flightNumber)
            .containsEntry("scheduledDepartureDate", date.toString())
            .containsEntry("std", std.toString())
            .containsEntry("sta", sta.toString()));
    }

    private List<OutgoingMessage> newFlms() {
        return traffic.partnerMessages().stream()
            .filter(message -> message.getType() == MessageType.FLM && !before.contains(message))
            .toList();
    }

    /** What Flight Operations sends for a new flight: the scheduled departure date is the UTC date of the STD. */
    private static Map<String, Object> flightOperationsAnnounces(Map<String, Object> flight) {
        Instant std = Instant.parse(String.valueOf(flight.get("std")));
        Map<String, Object> announced = Messages.flightAnnounced(String.valueOf(flight.get("flightNumber")),
            LocalDate.ofInstant(std, ZoneOffset.UTC), String.valueOf(flight.get("departureAirport")),
            String.valueOf(flight.get("arrivalAirport")), String.valueOf(flight.get("std")),
            String.valueOf(flight.get("sta")));
        announced.put("registration", flight.get("registration"));
        return announced;
    }
}
