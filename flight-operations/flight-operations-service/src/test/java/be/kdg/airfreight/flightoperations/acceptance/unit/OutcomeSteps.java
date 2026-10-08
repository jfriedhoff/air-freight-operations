package be.kdg.airfreight.flightoperations.acceptance.unit;

import io.cucumber.java.en.Then;

/**
 * Then steps that several stories share: accepted or refused. A refusal has to be for the reason the scenario
 * causes: the step that causes it (usually the When step) sets that reason.
 */
public class OutcomeSteps {
    private static final String DELAY_FIRST = "delay first";
    private static final String IN_THE_FUTURE = "cannot be in the future";

    private final FlightWorld world;

    public OutcomeSteps(FlightWorld world) {
        this.world = world;
    }

    @Then("it/that is {word}")
    @Then("the registration is {word}")
    public void itIs(String outcome) {
        switch (outcome) {
            case "accepted" -> world.assertAccepted();
            case "refused" -> world.assertRefusedForTheExpectedReason();
            default -> throw new IllegalArgumentException("Unknown outcome: " + outcome);
        }
    }

    @Then("it/that is refused with an explanation")
    @Then("the registration is refused with an explanation")
    public void itIsRefusedWithAnExplanation() {
        world.assertRefusedForTheExpectedReason();
    }

    /** The explanation names another reason than the usual one of the When step. */
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
