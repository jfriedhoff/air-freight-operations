package be.kdg.airfreight.loadplanning.acceptance.integration;

import be.kdg.airfreight.loadplanning.infrastructure.flightcargo.jpa.JpaFlightCargoRepository;
import be.kdg.airfreight.loadplanning.infrastructure.messaging.OutboxEventRepository;
import io.cucumber.java.After;

/** Every scenario starts with an empty test database. */
public class DatabaseCleanUp {
    private final JpaFlightCargoRepository cargos;
    private final OutboxEventRepository outbox;

    public DatabaseCleanUp(JpaFlightCargoRepository cargos, OutboxEventRepository outbox) {
        this.cargos = cargos;
        this.outbox = outbox;
    }

    /** Deleting a cargo deletes its ULDs with it. */
    @After
    public void cleanUp() {
        cargos.deleteAll();
        outbox.deleteAll();
    }
}
