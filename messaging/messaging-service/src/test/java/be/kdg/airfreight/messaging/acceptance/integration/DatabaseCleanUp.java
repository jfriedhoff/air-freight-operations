package be.kdg.airfreight.messaging.acceptance.integration;

import be.kdg.airfreight.messaging.infrastructure.messaging.OutboxEventRepository;
import be.kdg.airfreight.messaging.infrastructure.persistence.jpa.JpaOutgoingMessageRepository;
import be.kdg.airfreight.messaging.infrastructure.persistence.jpa.JpaReceivedMessageRepository;
import io.cucumber.java.After;

/** Every scenario starts with an empty test database. */
public class DatabaseCleanUp {
    private final JpaReceivedMessageRepository received;
    private final JpaOutgoingMessageRepository outgoing;
    private final OutboxEventRepository outbox;

    public DatabaseCleanUp(JpaReceivedMessageRepository received, JpaOutgoingMessageRepository outgoing,
                           OutboxEventRepository outbox) {
        this.received = received;
        this.outgoing = outgoing;
        this.outbox = outbox;
    }

    @After
    public void cleanUp() {
        received.deleteAll();
        outgoing.deleteAll();
        outbox.deleteAll();
    }
}
