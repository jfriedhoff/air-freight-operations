package be.kdg.airfreight.loadplanning.acceptance.unit;

import be.kdg.airfreight.loadplanning.domain.flightcargo.LateralBalanceLimit;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadingPolicy;
import io.cucumber.java.en.When;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Story #37: a policy value is a value object of the domain, which refuses a value that cannot be right. The
 * other time of the loading period keeps its default, so each time is checked on its own.
 */
public class PolicyValueSteps {
    private static final String NEGATIVE = "cannot be negative";

    private final CargoWorld world;

    public PolicyValueSteps(CargoWorld world) {
        this.world = world;
    }

    /** "-1 h", "0 min" and "-1%", as the examples write them. */
    @When("the operations manager sets {} to {}")
    public void theOperationsManagerSets(String value, String newValue) {
        int amount = Integer.parseInt(newValue.split("[ %]")[0]);
        LoadingPolicy defaults = CargoWorld.POLICY;
        world.expectedRefusal = NEGATIVE;
        world.attempt(() -> {
            switch (value) {
                case "the start of the loading period" ->
                    new LoadingPolicy(Duration.ofHours(amount), defaults.closesBeforeEstimatedDeparture());
                case "the end of the loading period" ->
                    new LoadingPolicy(defaults.opensBeforeScheduledDeparture(), Duration.ofMinutes(amount));
                case "the allowed lateral balance difference" ->
                    new LateralBalanceLimit(BigDecimal.valueOf(amount, 2));
                default -> throw new IllegalArgumentException("Unknown policy value: " + value);
            }
        });
    }
}
