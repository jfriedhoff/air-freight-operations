package be.kdg.airfreight.loadplanning.domain.flightcargo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import be.kdg.airfreight.loadplanning.domain.InvalidValueException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FloorPlanTest {
    /** Twelve positions, added in an order that is neither alphabetical nor by hash: an unordered map loses it. */
    private static final List<String> NOSE_TO_TAIL =
        List.of("X1", "A1", "ML", "MR", "CL", "CR", "UL", "UR", "B1", "DL", "DR", "A2");

    @Test
    void thePositionsKeepTheOrderOfTheFloorPlan() {
        Map<LoadPosition, List<Route>> routes = new LinkedHashMap<>();
        NOSE_TO_TAIL.forEach(name -> routes.put(new LoadPosition(name, null), List.of(new Route(List.of()))));

        FloorPlan floorPlan = new FloorPlan(new AircraftType("TEST"), routes, new LoadPosition("CL", null));

        assertThat(floorPlan.positions()).extracting(LoadPosition::name).containsExactlyElementsOf(NOSE_TO_TAIL);
    }

    @Test
    void theCargoDoorIsAtOneOfThePositionsOfTheFloorPlan() {
        LoadPosition cl = new LoadPosition("CL", Side.LEFT);

        FloorPlan floorPlan = new FloorPlan(new AircraftType("TEST"), Map.of(cl, List.of(new Route(List.of()))), cl);

        assertThat(floorPlan.door()).isEqualTo(cl);
    }

    @Test
    void aCargoDoorAtAPositionThatIsNotOnTheFloorPlanIsRefused() {
        LoadPosition cl = new LoadPosition("CL", Side.LEFT);
        Map<LoadPosition, List<Route>> routes = Map.of(cl, List.of(new Route(List.of())));

        assertThatThrownBy(() -> new FloorPlan(new AircraftType("TEST"), routes, new LoadPosition("RL", Side.LEFT)))
            .isInstanceOf(InvalidValueException.class)
            .hasMessage("The cargo door of aircraft type TEST must be at one of its load positions, not at RL");
    }
}
