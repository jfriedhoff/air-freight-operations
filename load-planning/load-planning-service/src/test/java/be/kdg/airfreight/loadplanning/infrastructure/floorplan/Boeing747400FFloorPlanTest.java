package be.kdg.airfreight.loadplanning.infrastructure.floorplan;

import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadPosition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Boeing747400FFloorPlanTest {
    @Test
    void theMainDeckHasExactlyThe34LoadPositionsFromNoseToTail() {
        assertThat(Boeing747400FFloorPlan.build().positions()).extracting(LoadPosition::name).containsExactly(
            "A1", "A2", "B1",
            "CL", "CR", "DL", "DR", "EL", "ER", "FL", "FR", "GL", "GR", "HL", "HR", "JL", "JR", "KL", "KR",
            "LL", "LR", "ML", "MR", "PL", "PR", "RL", "RR", "SL", "SR", "TL", "TR", "UL", "UR",
            "X");
    }
}
