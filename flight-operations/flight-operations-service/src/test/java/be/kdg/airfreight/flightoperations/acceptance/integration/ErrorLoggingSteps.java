package be.kdg.airfreight.flightoperations.acceptance.integration;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import be.kdg.airfreight.flightoperations.DatabaseFaults;
import be.kdg.airfreight.flightoperations.TestHelper;
import be.kdg.airfreight.flightoperations.application.FlightSchedulingService;
import be.kdg.airfreight.flightoperations.application.command.ScheduleFlightCommand;
import be.kdg.airfreight.flightoperations.domain.flight.AircraftRegistration;
import be.kdg.airfreight.flightoperations.domain.flight.AirportCode;
import be.kdg.airfreight.flightoperations.domain.flight.FlightId;
import be.kdg.airfreight.flightoperations.domain.flight.FlightNumber;
import be.kdg.airfreight.flightoperations.domain.flightplanner.FlightPlannerId;
import be.kdg.airfreight.flightoperations.infrastructure.messaging.OutboxRelay;
import ch.qos.logback.classic.Level;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.Lifecycle;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Story #39 in Flight Operations: what is logged when the database, RabbitMQ or an incoming message goes wrong.
 * Each Given remembers the fragment of the log line it causes, so the Then step finds that line and not another
 * error that happens to be logged in the same scenario.
 * The tests of Flight Operations start no listener and no relay timer: a scenario that needs them starts the
 * listener itself, or runs the relay as its timer would.
 */
public class ErrorLoggingSteps {
    private static final Duration WAIT = Duration.ofSeconds(20);
    private static final String NOT_PUBLISHED = "could not be published and stays in the outbox";

    private final MockMvc mockMvc;
    private final TestHelper testHelper;
    private final LogCapture logs;
    private final DatabaseFaults databaseFaults;
    private final RabbitMqOutageSteps rabbitMqOutage;
    private final OutboxRelay relay;
    private final RabbitTemplate rabbit;
    private final AmqpAdmin amqpAdmin;
    private final RabbitListenerEndpointRegistry listeners;
    private final FlightSchedulingService scheduling;
    private final String eventsExchange;
    private final String messagingEventsExchange;
    private final String queue;
    private final String deadLetterQueue;

    private FlightId flightId;
    private FlightPlannerId flightPlanner;
    /** The part of the log line the scenario has to cause. */
    private String expectedFragment;
    private boolean exchangeDeleted;

    public ErrorLoggingSteps(MockMvc mockMvc, TestHelper testHelper, LogCapture logs, DatabaseFaults databaseFaults,
                             RabbitMqOutageSteps rabbitMqOutage, OutboxRelay relay, RabbitTemplate rabbit,
                             AmqpAdmin amqpAdmin, RabbitListenerEndpointRegistry listeners,
                             FlightSchedulingService scheduling,
                             @Value("${flight-operations.amqp.events-exchange}") String eventsExchange,
                             @Value("${flight-operations.amqp.messaging-events-exchange}") String messagingEventsExchange,
                             @Value("${flight-operations.amqp.queue}") String queue,
                             @Value("${flight-operations.amqp.dead-letter-queue}") String deadLetterQueue) {
        this.mockMvc = mockMvc;
        this.testHelper = testHelper;
        this.logs = logs;
        this.databaseFaults = databaseFaults;
        this.rabbitMqOutage = rabbitMqOutage;
        this.relay = relay;
        this.rabbit = rabbit;
        this.amqpAdmin = amqpAdmin;
        this.listeners = listeners;
        this.scheduling = scheduling;
        this.eventsExchange = eventsExchange;
        this.messagingEventsExchange = messagingEventsExchange;
        this.queue = queue;
        this.deadLetterQueue = deadLetterQueue;
    }

    /**
     * The exchange and its bindings come back, and the listener stops with nothing left on its queue or its dead
     * letter queue. Before the clean-up of the database (order 10000), so the listener writes nothing after it.
     */
    @After(order = 15000)
    public void putRabbitMqBack() {
        if (exchangeDeleted) {
            ((RabbitAdmin) amqpAdmin).initialize();
        }
        if (listeners.isRunning()) {
            listeners.getListenerContainers().forEach(Lifecycle::stop);
            amqpAdmin.purgeQueue(queue, false);
            amqpAdmin.purgeQueue(deadLetterQueue, false);
        }
    }

    @Given("the database is unreachable")
    public void theDatabaseIsUnreachable() {
        prepareAFlightAndAPlanner();
        databaseFaults.makeUnreachable();
        expectedFragment = "Database unavailable for POST /api/flights/";
    }

    @Given("a database operation fails unexpectedly")
    public void aDatabaseOperationFails() {
        prepareAFlightAndAPlanner();
        databaseFaults.makeOperationsFail();
        expectedFragment = "Database operation failed for POST /api/flights/";
    }

    /** RabbitMQ is there, but the exchange the relay publishes on is not: RabbitMQ does not take the event. */
    @Given("a message cannot be put on the queue")
    public void aMessageCannotBePutOnTheQueue() {
        amqpAdmin.deleteExchange(eventsExchange);
        exchangeDeleted = true;
        expectedFragment = NOT_PUBLISHED;
    }

    /**
     * The flight planner changes a flight: that needs the database, and the event it causes needs RabbitMQ. The relay
     * runs only after a change that was accepted; after a refusal there is nothing to publish.
     */
    @When("the system notices it")
    public void theSystemNoticesIt() throws Exception {
        prepareAFlightAndAPlanner();
        MvcResult answer = mockMvc.perform(post("/api/flights/{id}/delays", flightId.id())
                .with(jwt().jwt(token -> token.subject(flightPlanner.id().toString()))
                    .authorities(new SimpleGrantedAuthority("ROLE_flight-planner")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"kind": "DEPARTURE", "minutes": 25, "reason": "93", "description": "Late inbound aircraft"}"""))
            .andReturn();
        if (answer.getResponse().getStatus() < 300) {
            relay.sendPending();
        }
    }

    /** RabbitMqOutageSteps breaks the connection for story 28 too, so its fragment is chosen here. */
    @Then("an error is logged at error level, with enough detail to find the cause")
    public void anErrorIsLogged() {
        String fragment = expectedFragment != null ? expectedFragment : NOT_PUBLISHED;
        assertThat(expectedFragment != null || rabbitMqOutage.isActive()).as("a failure of the scenario").isTrue();
        logs.assertLoggedWithCause(Level.ERROR, fragment);
    }

    @When("a message with invalid data arrives")
    public void aMessageWithInvalidDataArrives() {
        UUID messageId = UUID.randomUUID();
        startTheListener();
        publish(flightScheduled(messageId, "221"));
        expectedFragment = messageId.toString();
    }

    /** Messaging can deliver the same message twice; the second one finds the flight it created. */
    @When("a message arrives twice")
    public void aMessageArrivesTwice() {
        startTheListener();
        Message message = flightScheduled(UUID.randomUUID(), "HJ221");
        publish(message);
        publish(message);
        expectedFragment = "Flight HJ221 of 2026-01-09 from BRU already exists";
    }

    @When("an SSM announces a flight that already exists")
    public void anSsmAnnouncesAFlightThatAlreadyExists() {
        testHelper.savePlannedFlight();
        scheduling.scheduleFlight(new ScheduleFlightCommand(new FlightNumber("HJ221"), new AirportCode("BRU"),
            new AirportCode("ATH"), new AircraftRegistration("SXBAA"),
            TestHelper.schedule(LocalDate.of(2026, 1, 9), LocalTime.of(6, 30))));
        expectedFragment = "Flight HJ221 of 2026-01-09 from BRU already exists";
    }

    /** The API names a flight by its id: the log line names the id nobody knows. */
    @When("someone tries to change a flight that does not exist")
    public void someoneTriesToChangeAFlightThatDoesNotExist() throws Exception {
        flightPlanner = testHelper.saveFlightPlanner();
        UUID unknown = UUID.randomUUID();
        mockMvc.perform(post("/api/flights/{id}/delays", unknown)
            .with(jwt().jwt(token -> token.subject(flightPlanner.id().toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_flight-planner")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"kind": "DEPARTURE", "minutes": 25, "reason": "93", "description": "Late inbound aircraft"}"""));
        expectedFragment = unknown.toString();
    }

    /** The listener handles a message on its own thread: wait until the line is there. */
    @Then("that is logged at level {word}, with the identification of the message or the flight")
    public void thatIsLoggedAtLevel(String level) {
        Level expected = Level.valueOf(level.toUpperCase());
        await().atMost(WAIT).untilAsserted(() -> logs.assertLogged(expected, expectedFragment));
    }

    private void prepareAFlightAndAPlanner() {
        if (flightId == null) {
            flightId = testHelper.savePlannedFlight().getId();
            flightPlanner = testHelper.saveFlightPlanner();
        }
    }

    /** A message an aborted run left on the queue would be handled first: the queue starts empty. */
    private void startTheListener() {
        amqpAdmin.purgeQueue(queue, false);
        listeners.getListenerContainers().forEach(Lifecycle::start);
    }

    private void publish(Message message) {
        rabbit.send(messagingEventsExchange, "messaging.flight.scheduled", message);
    }

    /** A FlightScheduled as Messaging sends it; flight number 221 has no airline code, so it is invalid. */
    private static Message flightScheduled(UUID messageId, String flightNumber) {
        String body = """
            {"messageId": "%s", "type": "messaging.flight.scheduled", "flightNumber": "%s",
             "departureAirport": "BRU", "arrivalAirport": "ATH", "registration": "SXBAA",
             "std": "2026-01-09T06:30:00Z", "sta": "2026-01-09T09:45:00Z"}""".formatted(messageId, flightNumber);
        return MessageBuilder.withBody(body.getBytes(StandardCharsets.UTF_8))
            .setContentType(MediaType.APPLICATION_JSON_VALUE)
            .setMessageId(messageId.toString())
            .build();
    }
}
