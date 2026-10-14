package be.kdg.airfreight.flightoperations.acceptance.unit;

import be.kdg.airfreight.flightoperations.domain.flight.ActualTimeTolerance;
import be.kdg.airfreight.flightoperations.domain.flight.RetentionPeriod;
import io.cucumber.java.en.When;

import java.time.Duration;

/** Story #37: a policy value is a value object of the domain, which refuses a value that cannot be right. */
public class PolicyValueSteps {
    private static final String NEGATIVE = "cannot be negative";

    private final FlightWorld world;

    public PolicyValueSteps(FlightWorld world) {
        this.world = world;
    }

    /** "-1 min" and "0 days", as the examples write them. */
    @When("the operations manager sets {} to {}")
    public void theOperationsManagerSets(String value, String newValue) {
        int amount = Integer.parseInt(newValue.split(" ")[0]);
        world.expectedRefusal = NEGATIVE;
        world.attempt(() -> {
            switch (value) {
                case "the tolerance for actual times" -> new ActualTimeTolerance(Duration.ofMinutes(amount));
                case "the retention period" -> new RetentionPeriod(amount);
                default -> throw new IllegalArgumentException("Unknown policy value: " + value);
            }
        });
    }
}
