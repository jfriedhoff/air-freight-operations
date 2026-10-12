package be.kdg.airfreight.loadplanning.acceptance.integration;

import ch.qos.logback.classic.Level;
import io.cucumber.java.en.Then;

/** Messages that Load Planning cannot process: set aside on its dead letter queue and logged. */
public class DeadLetterSteps {
    private final MessagingEvents messaging;
    private final MessageWorld world;
    private final LogCapture logs;

    public DeadLetterSteps(MessagingEvents messaging, MessageWorld world, LogCapture logs) {
        this.messaging = messaging;
        this.world = world;
        this.logs = logs;
    }

    /** The step that sends the message sets the reason it must be set aside for. */
    @Then("the message is set aside and logged with its identification and the reason")
    @Then("the message is set aside and logged")
    public void theMessageIsSetAsideAndLogged() {
        messaging.assertSetAside(world.getMessageId());
        logs.assertLogged(Level.WARN, world.getMessageId().toString(), world.getExpectedReason());
    }
}
