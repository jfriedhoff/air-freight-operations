package be.kdg.airfreight.flightoperations.acceptance.unit;

import io.cucumber.java.en.Then;

import static org.assertj.core.api.Assertions.assertThat;

/** Then steps that several stories share: accepted or refused, and for which reason. */
public class OutcomeSteps {
    private static final String DELAY_FIRST = "delay first";
    private static final String IN_THE_FUTURE = "cannot be in the future";

    private final FlightWorld world;

    public OutcomeSteps(FlightWorld world) {
        this.world = world;
    }

    @Then("it/that is {word}")
    public void itIs(String result) {
        switch (result) {
            case "accepted" -> assertThat(world.error).isNull();
            case "refused" -> world.assertRefusedForTheExpectedReason();
            default -> throw new IllegalArgumentException("Unknown result: " + result);
        }
    }

    @Then("it/that is refused with an explanation")
    public void itIsRefusedWithAnExplanation() {
        world.assertRefusedForTheExpectedReason();
    }

    @Then("the registration is accepted")
    public void theRegistrationIsAccepted() {
        assertThat(world.error).isNull();
    }

    @Then("the registration is refused")
    public void theRegistrationIsRefused() {
        world.assertRefusedForTheExpectedReason();
    }

    @Then("the registration is refused with an explanation")
    public void theRegistrationIsRefusedWithAnExplanation() {
        world.assertRefusedForTheExpectedReason();
    }

    @Then("the registration is refused: {}")
    public void theRegistrationIsRefusedBecause(String explanation) {
        world.expectedRefusal = switch (explanation) {
            case "enter a delay first" -> DELAY_FIRST;
            case "lies in the future" -> IN_THE_FUTURE;
            default -> throw new IllegalArgumentException("Unknown explanation: " + explanation);
        };
        world.assertRefusedForTheExpectedReason();
    }
}
