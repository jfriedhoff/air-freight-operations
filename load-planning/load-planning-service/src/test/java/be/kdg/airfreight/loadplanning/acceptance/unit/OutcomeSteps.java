package be.kdg.airfreight.loadplanning.acceptance.unit;

import io.cucumber.java.en.Then;

/**
 * Then steps that several stories share: accepted or refused. A refusal has to be for the reason the scenario
 * causes: the step that causes it (such as "it is ... UTC" or "... is already on CL") sets that reason.
 */
public class OutcomeSteps {
    private final CargoWorld world;

    public OutcomeSteps(CargoWorld world) {
        this.world = world;
    }

    @Then("it/that is {word}")
    public void itIs(String outcome) {
        switch (outcome) {
            case "accepted" -> world.assertAccepted();
            case "refused" -> world.assertRefusedForTheExpectedReason();
            default -> throw new IllegalArgumentException("Unknown outcome: " + outcome);
        }
    }

    @Then("it/that is refused with an explanation")
    public void itIsRefusedWithAnExplanation() {
        world.assertRefusedForTheExpectedReason();
    }
}
