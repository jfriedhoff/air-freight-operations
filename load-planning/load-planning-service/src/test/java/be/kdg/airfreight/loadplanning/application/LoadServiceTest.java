package be.kdg.airfreight.loadplanning.application;

import be.kdg.airfreight.loadplanning.TestClock;
import be.kdg.airfreight.loadplanning.TestHelper;
import be.kdg.airfreight.loadplanning.application.command.FinalizeLoadCommand;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargoId;
import be.kdg.airfreight.loadplanning.domain.flightcargo.WeightUnit;
import be.kdg.airfreight.loadplanning.domain.loadplanner.LoadPlannerId;
import be.kdg.airfreight.loadplanning.domain.loadplanner.UnknownLoadPlannerException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Finalizing has no endpoint yet, so the use case is called directly. It is the one change that cannot be
 * undone: it must record a load planner that exists, like assigning and clearing.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestClock.class)
class LoadServiceTest {
    @Autowired
    private LoadService loadService;
    @Autowired
    private TestHelper testHelper;
    @Autowired
    private TestClock clock;

    private FlightCargoId id;

    /** One ULD in the nose: it counts for neither side and nothing is in its way, so every hard check passes. */
    @BeforeEach
    void theCargoOfHj221WithItsOnlyUldOnA1() {
        clock.set(TestHelper.IN_THE_LOADING_PERIOD);
        id = testHelper.saveCargo().getId();
        testHelper.book(id, "AKE16689R7", "1250", WeightUnit.KG, "ATH");
        testHelper.place(id, "AKE16689R7", "A1");
    }

    @AfterEach
    void cleanUp() {
        testHelper.cleanUp();
        clock.reset();
    }

    @Test
    void aLoadPlannerWhoIsNotRegisteredCannotFinalizeTheLoad() {
        LoadPlannerId notRegistered = LoadPlannerId.create();

        assertThatThrownBy(() -> loadService.finalizeLoad(new FinalizeLoadCommand(id, notRegistered)))
            .isInstanceOf(UnknownLoadPlannerException.class)
            .hasMessage("You are not registered as a load planner");

        assertThat(testHelper.find(id).isFinalized()).isFalse();
        assertThat(testHelper.outboxCount()).as("no CPM leaves for a refused finalization").isZero();
    }

    @Test
    void aRegisteredLoadPlannerFinalizesTheLoad() {
        LoadPlannerId registered = testHelper.saveLoadPlanner("Test load planner");

        FinalizeOutcome outcome = loadService.finalizeLoad(new FinalizeLoadCommand(id, registered));

        assertThat(outcome.finalized()).isTrue();
        assertThat(testHelper.find(id).isFinalized()).isTrue();
        assertThat(testHelper.find(id).getFinalizedBy()).isEqualTo(registered);
    }
}
