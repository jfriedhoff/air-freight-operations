package be.kdg.airfreight.loadplanning.domain.flightcargo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FloorPlanTest {
    /** Twelve positions, added in an order that is neither alphabetical nor by hash: an unordered map loses it. */
    private static final List<String> NOSE_TO_TAIL =
        List.of("X1", "A1", "ML", "MR", "CL", "CR", "UL", "UR", "B1", "DL", "DR", "A2");

    @Test
    void thePositionsKeepTheOrderOfTheFloorPlan() {
        Map<LoadPosition, List<Route>> routes = new LinkedHashMap<>();
        NOSE_TO_TAIL.forEach(name -> routes.put(new LoadPosition(name, null), List.of(new Route(List.of()))));

        FloorPlan floorPlan = new FloorPlan(new AircraftType("TEST"), routes);

        assertThat(floorPlan.positions()).extracting(LoadPosition::name).containsExactlyElementsOf(NOSE_TO_TAIL);
    }
}
