package be.kdg.airfreight.loadplanning.acceptance.unit;

import be.kdg.airfreight.loadplanning.domain.flightcargo.UldCode;
import io.cucumber.java.en.Given;

import static be.kdg.airfreight.loadplanning.acceptance.unit.CargoWorld.ANY_WEIGHT;

/** Cargo that is already on a load position before the When step. */
public class CargoOnBoardSteps {
    private static final String OCCUPIED = "already holds ULD ";

    private final CargoWorld world;

    public CargoOnBoardSteps(CargoWorld world) {
        this.world = world;
    }

    @Given("the L positions hold {weight} in total and the R positions {weight}")
    @Given("the left side holds {weight} and the right side {weight}")
    public void theLeftAndRightSideHold(String left, String right) {
        world.loadWeightOn("CL", left);
        world.loadWeightOn("CR", right);
    }

    @Given("{word} holds {weight} and {word} {weight}")
    public void twoPositionsHold(String first, String firstWeight, String second, String secondWeight) {
        world.loadWeightOn(first, firstWeight);
        world.loadWeightOn(second, secondWeight);
    }

    @Given("{word} holds {weight}, {word} {weight} and {word} {weight}")
    public void threePositionsHold(String first, String firstWeight, String second, String secondWeight,
                                   String third, String thirdWeight) {
        world.loadWeightOn(first, firstWeight);
        world.loadWeightOn(second, secondWeight);
        world.loadWeightOn(third, thirdWeight);
    }

    /** Placing another ULD there is refused because this one holds the position. */
    @Given("{uld} is already on {word}")
    public void isAlreadyOn(UldCode code, String position) {
        world.book(code, ANY_WEIGHT);
        world.place(code, position);
        world.expectedRefusal = OCCUPIED + code.value();
    }

    @Given("{uld} and {uld} are booked and both have a load position")
    public void areBookedAndBothHaveALoadPosition(UldCode first, UldCode second) {
        world.book(first, ANY_WEIGHT);
        world.place(first, "CL");
        world.book(second, ANY_WEIGHT);
        world.place(second, "CR");
    }

    @Given("{uld} for {word} is on {word} and {uld} for {word} is on {word}")
    public void twoUldsAreOn(UldCode firstCode, String firstDestination, String firstPosition,
                             UldCode secondCode, String secondDestination, String secondPosition) {
        world.load(firstCode, ANY_WEIGHT, firstDestination, firstPosition);
        world.load(secondCode, ANY_WEIGHT, secondDestination, secondPosition);
    }

    @Given("{uld}, {weight} for {word}, is on {word} and {uld}, {weight} for {word}, is on {word}")
    public void twoUldsWithTheirWeightAreOn(UldCode firstCode, String firstWeight, String firstDestination,
                                           String firstPosition, UldCode secondCode, String secondWeight,
                                           String secondDestination, String secondPosition) {
        world.load(firstCode, firstWeight, firstDestination, firstPosition);
        world.load(secondCode, secondWeight, secondDestination, secondPosition);
    }

    /** See LoadDescription for the texts, such as "ATH on RR and UR, FRA on UL". */
    @Given("^([A-Z]{3} (?:on|without) .+)$")
    public void theLoadIs(String description) {
        for (LoadDescription.Uld uld : LoadDescription.parse(description)) {
            UldCode code = world.nextCode();
            world.book(code, ANY_WEIGHT, uld.destination());
            if (uld.position() != null) {
                world.place(code, uld.position());
            }
        }
    }

    @Given("only {word} on board")
    public void onlyOnBoard(String destination) {
        world.load(world.nextCode(), ANY_WEIGHT, destination, "RL");
    }
}
