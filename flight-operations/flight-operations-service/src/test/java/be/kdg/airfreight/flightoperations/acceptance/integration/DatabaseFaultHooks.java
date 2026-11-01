package be.kdg.airfreight.flightoperations.acceptance.integration;

import be.kdg.airfreight.flightoperations.DatabaseFaults;
import io.cucumber.java.After;

/**
 * A scenario that broke the database gets it back before the other After hooks empty it: an After hook with a
 * higher order runs earlier.
 */
public class DatabaseFaultHooks {
    private final DatabaseFaults databaseFaults;

    public DatabaseFaultHooks(DatabaseFaults databaseFaults) {
        this.databaseFaults = databaseFaults;
    }

    @After(order = 20000)
    public void restoreTheDatabase() {
        databaseFaults.restore();
    }
}
