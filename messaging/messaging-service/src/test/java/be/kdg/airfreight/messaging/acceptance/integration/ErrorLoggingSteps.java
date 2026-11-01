package be.kdg.airfreight.messaging.acceptance.integration;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;

import be.kdg.airfreight.messaging.DatabaseFaults;
import be.kdg.airfreight.messaging.acceptance.integration.MessagingTraffic.Stream;
import be.kdg.airfreight.messaging.application.FlightEventService;
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
import tools.jackson.databind.json.JsonMapper;

import static org.awaitility.Awaitility.await;

/**
 * Story #39 in Messaging: what is logged when the database, RabbitMQ or an incoming message goes wrong.
 * Messaging has no API, so it notices a failure in one of two places. A broken database: when a message arrives,
 * the listener fails until its retries run out, and the recoverer logs the message. A broken RabbitMQ: when a
 * partner message has to go out, the relay cannot send it. Each Given chooses which of the two happens, and the
 * fragment of the log line it causes, so the Then step finds that line and not another error of the same scenario.
 */
public class ErrorLoggingSteps {
    /** The retries of the listener take 1 + 2 + 4 s before the recoverer runs. */
    private static final Duration WAIT = Duration.ofSeconds(30);
    private static final Duration LISTENERS_BACK = Duration.ofSeconds(15);
    private static final int NOTHING_LISTENS = 1;
    private static final String NOT_SENT = "could not be sent and stays in the outbox";

    private final MessagingTraffic traffic;
    private final JsonMapper mapper;
    private final LogCapture logs;
    private final DatabaseFaults databaseFaults;
    private final CachingConnectionFactory connectionFactory;
    private final AmqpAdmin amqpAdmin;
    private final RabbitListenerEndpointRegistry listeners;
    private final FlightEventService flightEvents;
    private final String partnerMessagesExchange;

    /** How Messaging notices the failure of this scenario. */
    private Runnable noticing;
    /** The part of the log line the scenario has to cause. */
    private String expectedFragment;
    /** Where RabbitMQ really is, after the scenario made it unreachable. */
    private String rabbitMqAddress;
    private boolean exchangeDeleted;

    public ErrorLoggingSteps(MessagingTraffic traffic, JsonMapper mapper, LogCapture logs,
                             DatabaseFaults databaseFaults, CachingConnectionFactory connectionFactory,
                             AmqpAdmin amqpAdmin, RabbitListenerEndpointRegistry listeners,
                             FlightEventService flightEvents,
                             @Value("${messaging.amqp.partner-messages-exchange}") String partnerMessagesExchange) {
        this.traffic = traffic;
        this.mapper = mapper;
        this.logs = logs;
        this.databaseFaults = databaseFaults;
        this.connectionFactory = connectionFactory;
        this.amqpAdmin = amqpAdmin;
        this.listeners = listeners;
        this.flightEvents = flightEvents;
        this.partnerMessagesExchange = partnerMessagesExchange;
    }

    /** RabbitMQ, the exchange and its bindings come back for the next scenario. */
    @After
    public void putRabbitMqBack() {
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
        databaseFaults.makeUnreachable();
        noticing = this::aFlightEventArrives;
    }

    @Given("a database operation fails unexpectedly")
    public void aDatabaseOperationFails() {
        databaseFaults.makeOperationsFail();
        noticing = this::aFlightEventArrives;
    }

    /** Every new connection goes to a port where nothing listens, as in story 28 of Flight Operations. */
    @Given("RabbitMQ is unreachable")
    public void rabbitMqIsUnreachable() {
        rabbitMqAddress = connectionFactory.getHost() + ":" + connectionFactory.getPort();
        connectionFactory.setAddresses(connectionFactory.getHost() + ":" + NOTHING_LISTENS);
        connectionFactory.resetConnection();
        noticing = this::aPartnerMessageHasToGoOut;
    }

    /** RabbitMQ is there, but the exchange of the partners is not: RabbitMQ does not take the telegram. */
    @Given("a message cannot be put on the queue")
    public void aMessageCannotBePutOnTheQueue() {
        amqpAdmin.deleteExchange(partnerMessagesExchange);
        exchangeDeleted = true;
        noticing = this::aPartnerMessageHasToGoOut;
    }

    @When("the system notices it")
    public void theSystemNoticesIt() {
        noticing.run();
    }

    @Then("an error is logged at error level, with enough detail to find the cause")
    public void anErrorIsLogged() {
        await().atMost(WAIT).untilAsserted(() -> logs.assertLoggedWithCause(Level.ERROR, expectedFragment));
    }

    @When("a message with invalid data arrives")
    public void aMessageWithInvalidDataArrives() {
        Map<String, Object> message = Messages.uldMessage();
        message.put("weight", -1250);
        traffic.send(Stream.BOOKINGS, message);
        expectedFragment = Messages.idOf(message) + " rejected and set aside";
    }

    @When("a message arrives twice")
    public void aMessageArrivesTwice() {
        Map<String, Object> message = Messages.ssm(LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 5));
        traffic.send(Stream.SCHEDULE, message);
        traffic.send(Stream.SCHEDULE, message);
        expectedFragment = "message " + Messages.idOf(message) + " ignored";
    }

    /** The listener handles a message on its own thread: wait until the line is there. */
    @Then("that is logged at level {word}, with the identification of the message or the flight")
    public void thatIsLoggedAtLevel(String level) {
        Level expected = Level.valueOf(level.toUpperCase());
        await().atMost(WAIT).untilAsserted(() -> logs.assertLogged(expected, expectedFragment));
    }

    /** Flight Operations reports a new ETD; with the database gone, its retries run out. */
    private void aFlightEventArrives() {
        Map<String, Object> event = newEtd();
        traffic.send(Stream.FLIGHTS, event);
        expectedFragment = "Message " + Messages.idOf(event) + " could not be processed after its retries";
    }

    /** Messaging processes a new ETD as its listener would, so an MVT-ED and an MVT-EA wait for the relay. */
    private void aPartnerMessageHasToGoOut() {
        flightEvents.receiveFlightEvent(mapper.writeValueAsString(newEtd()));
        expectedFragment = NOT_SENT;
    }

    private static Map<String, Object> newEtd() {
        Map<String, Object> event = Messages.flightEvent("departure-estimate-changed");
        event.put("etd", "2026-01-09T06:45:00Z");
        return event;
    }
}
