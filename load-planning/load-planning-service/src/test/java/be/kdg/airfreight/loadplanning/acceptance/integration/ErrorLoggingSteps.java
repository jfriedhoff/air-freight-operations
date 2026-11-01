package be.kdg.airfreight.loadplanning.acceptance.integration;

import java.time.Duration;
import java.util.UUID;

import be.kdg.airfreight.loadplanning.DatabaseFaults;
import be.kdg.airfreight.loadplanning.TestClock;
import be.kdg.airfreight.loadplanning.TestHelper;
import be.kdg.airfreight.loadplanning.domain.flightcargo.AirportCode;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargoId;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightId;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightNumber;
import be.kdg.airfreight.loadplanning.domain.loadplanner.LoadPlannerId;
import ch.qos.logback.classic.Level;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.web.servlet.MockMvc;

import static be.kdg.airfreight.loadplanning.acceptance.integration.Logins.token;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Story #39 in Load Planning: what is logged when the database, RabbitMQ or an incoming message goes wrong.
 * Each step remembers the fragment of the log line it causes, so the Then step finds that line and not another
 * error that happens to be logged in the same scenario: the relay and the listener keep running meanwhile.
 */
public class ErrorLoggingSteps {
    private static final Duration WAIT = Duration.ofSeconds(30);
    private static final Duration LISTENERS_BACK = Duration.ofSeconds(15);
    private static final int NOTHING_LISTENS = 1;
    private static final String NOT_PUBLISHED = "could not be published and stays in the outbox";
    private static final FlightId HJ221 = new FlightId(new FlightNumber("HJ221"), TestHelper.DATE,
        new AirportCode("BRU"));

    private final MockMvc mockMvc;
    private final TestHelper testHelper;
    private final TestClock clock;
    private final LogCapture logs;
    private final DatabaseFaults databaseFaults;
    private final CachingConnectionFactory connectionFactory;
    private final AmqpAdmin amqpAdmin;
    private final RabbitListenerEndpointRegistry listeners;
    private final MessagingEvents messaging;
    private final String eventsExchange;

    private FlightCargoId cargoId;
    private LoadPlannerId loadPlanner;
    /** The part of the log line the scenario has to cause. */
    private String expectedFragment;
    /** Where RabbitMQ really is, after the scenario made it unreachable. */
    private String rabbitMqAddress;
    private boolean exchangeDeleted;

    public ErrorLoggingSteps(MockMvc mockMvc, TestHelper testHelper, TestClock clock, LogCapture logs,
                             DatabaseFaults databaseFaults, CachingConnectionFactory connectionFactory,
                             AmqpAdmin amqpAdmin, RabbitListenerEndpointRegistry listeners,
                             MessagingEvents messaging,
                             @Value("${load-planning.amqp.events-exchange}") String eventsExchange) {
        this.mockMvc = mockMvc;
        this.testHelper = testHelper;
        this.clock = clock;
        this.logs = logs;
        this.databaseFaults = databaseFaults;
        this.connectionFactory = connectionFactory;
        this.amqpAdmin = amqpAdmin;
        this.listeners = listeners;
        this.messaging = messaging;
        this.eventsExchange = eventsExchange;
    }

    /** RabbitMQ, the exchange and its bindings come back for the next scenario, and the clock tells the time again. */
    @After
    public void putRabbitMqBack() {
        clock.reset();
        if (rabbitMqAddress != null) {
            connectionFactory.setAddresses(rabbitMqAddress);
            connectionFactory.resetConnection();
            // the listeners reconnect on their own, within seconds: the next scenario starts with them back
            await().atMost(LISTENERS_BACK).until(() -> listeners.getListenerContainers().stream()
                .allMatch(container -> ((SimpleMessageListenerContainer) container).getActiveConsumerCount() > 0));
        }
        if (exchangeDeleted) {
            ((RabbitAdmin) amqpAdmin).initialize();
        }
    }

    @Given("the database is unreachable")
    public void theDatabaseIsUnreachable() {
        prepareACargoAndALoadPlanner();
        databaseFaults.makeUnreachable();
        expectedFragment = "Database unavailable for POST /api/flight-cargos/";
    }

    @Given("a database operation fails unexpectedly")
    public void aDatabaseOperationFails() {
        prepareACargoAndALoadPlanner();
        databaseFaults.makeOperationsFail();
        expectedFragment = "Database operation failed for POST /api/flight-cargos/";
    }

    /** Every new connection goes to a port where nothing listens, as in story 28 of Flight Operations. */
    @Given("RabbitMQ is unreachable")
    public void rabbitMqIsUnreachable() {
        rabbitMqAddress = connectionFactory.getHost() + ":" + connectionFactory.getPort();
        connectionFactory.setAddresses(connectionFactory.getHost() + ":" + NOTHING_LISTENS);
        connectionFactory.resetConnection();
        expectedFragment = NOT_PUBLISHED;
    }

    /** RabbitMQ is there, but the exchange the relay publishes on is not: RabbitMQ does not take the event. */
    @Given("a message cannot be put on the queue")
    public void aMessageCannotBePutOnTheQueue() {
        amqpAdmin.deleteExchange(eventsExchange);
        exchangeDeleted = true;
        expectedFragment = NOT_PUBLISHED;
    }

    /**
     * The load planner finalizes an empty load: that needs the database, and the event it causes needs RabbitMQ,
     * which the relay of Load Planning tries on its own timer.
     */
    @When("the system notices it")
    public void theSystemNoticesIt() throws Exception {
        prepareACargoAndALoadPlanner();
        mockMvc.perform(post("/api/flight-cargos/{id}/finalize", cargoId.id()).with(token(loadPlanner.id(),
            "load-planner")));
    }

    @Then("an error is logged at error level, with enough detail to find the cause")
    public void anErrorIsLogged() {
        await().atMost(WAIT).untilAsserted(() -> logs.assertLoggedWithCause(Level.ERROR, expectedFragment));
    }

    @When("a message with invalid data arrives")
    public void aMessageWithInvalidDataArrives() {
        UUID messageId = messaging.departureEstimateChangedWithoutAMoment(HJ221);
        expectedFragment = messageId.toString();
    }

    /**
     * Load Planning has no inbox (coach, 14/10/2026): it recognizes a second announcement of the same flight by
     * the cargo that flight already has, and logs it with the flight.
     */
    @When("a flight announcement arrives twice")
    public void aFlightAnnouncementArrivesTwice() {
        messaging.sendAgain(messaging.flightAnnounced(HJ221));
        expectedFragment = "Flight HJ221 of 2026-01-09 already has a cargo";
    }

    /** The listener handles a message on its own thread: wait until the line is there. */
    @Then("that is logged at level {word}, with the identification of the message or the flight")
    public void thatIsLoggedAtLevel(String level) {
        Level expected = Level.valueOf(level.toUpperCase());
        await().atMost(WAIT).untilAsserted(() -> logs.assertLogged(expected, expectedFragment));
    }

    private void prepareACargoAndALoadPlanner() {
        if (cargoId == null) {
            clock.set(TestHelper.IN_THE_LOADING_PERIOD);
            cargoId = testHelper.saveCargo().getId();
            loadPlanner = testHelper.saveLoadPlanner("Test load planner");
        }
    }
}
