package be.kdg.airfreight.loadplanning.acceptance.unit;

import be.kdg.airfreight.loadplanning.domain.flightcargo.UldCode;
import be.kdg.airfreight.loadplanning.domain.flightcargo.check.OffloadingCheck;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

public class OffloadingSteps {
    private static final String ANY_WEIGHT = "1000 kg";

    private final CargoWorld world;

    public OffloadingSteps(CargoWorld world) {
        this.world = world;
    }

    @Given("the flight goes to {word}")
    public void theFlightGoesTo(String airport) {
        world.goesTo(airport);
    }

    /**
     * "ATH on RR and UR, FRA on UL", "ATH without load position, FRA on RL" or "ATH on A1, A2 and B1, CL and CR free".
     * "ATH on" starts the ULDs for ATH, "A2 and B1" adds more for the same destination, and free positions stay empty.
     */
    @Given("^([A-Z]{3} (?:on|without) .+)$")
    public void theLoadIs(String description) {
        String destination = "";
        for (String part : description.split(", ")) {
            String[] words = part.split(" ", 3);
            if (part.endsWith(" free")) {
                continue;
            }
            if (part.endsWith(" without load position")) {
                world.book(world.nextCode(), ANY_WEIGHT, words[0]);
                continue;
            }
            String positions = part;
            if (words.length == 3 && words[1].equals("on")) {
                destination = words[0];
                positions = words[2];
            }
            for (String position : positions.split(" and ")) {
                loadFor(destination, position);
            }
        }
    }

    @Given("only {word} on board")
    public void onlyOnBoard(String destination) {
        loadFor(destination, "RL");
    }

    @Given("no cargo on board")
    public void noCargoOnBoard() {
        // the flight starts empty
    }

    @Given("{word} for {word} is on {word} and {word} for {word} is on {word}")
    public void twoUldsAreOn(String firstCode, String firstDestination, String firstPosition,
                             String secondCode, String secondDestination, String secondPosition) {
        loadFor(new UldCode(firstCode), firstDestination, firstPosition);
        loadFor(new UldCode(secondCode), secondDestination, secondPosition);
    }

    @When("the offloading is checked")
    public void theOffloadingIsChecked() {
        world.check(new OffloadingCheck());
    }

    @Then("the report states that {word} on {word} cannot be offloaded because {word} on {word} is in the way")
    public void theReportStates(String stuckCode, String stuckPosition, String blockingCode, String blockingPosition) {
        assertThat(world.result.reason()).isEqualTo(stuckCode + " on " + stuckPosition
            + " cannot be offloaded because " + blockingCode + " on " + blockingPosition + " is in the way");
    }

    private void loadFor(String destination, String position) {
        loadFor(world.nextCode(), destination, position);
    }

    private void loadFor(UldCode code, String destination, String position) {
        world.book(code, ANY_WEIGHT, destination);
        world.place(code, position);
    }
}
