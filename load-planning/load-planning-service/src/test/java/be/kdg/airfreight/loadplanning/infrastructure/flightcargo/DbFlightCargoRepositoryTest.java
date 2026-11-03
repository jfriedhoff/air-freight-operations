package be.kdg.airfreight.loadplanning.infrastructure.flightcargo;

import be.kdg.airfreight.loadplanning.TestClock;
import be.kdg.airfreight.loadplanning.TestHelper;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargo;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargoId;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargoRepository;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FloorPlan;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadingPolicy;
import be.kdg.airfreight.loadplanning.domain.flightcargo.Uld;
import be.kdg.airfreight.loadplanning.domain.flightcargo.UldCode;
import be.kdg.airfreight.loadplanning.domain.flightcargo.WeightUnit;
import be.kdg.airfreight.loadplanning.domain.loadplanner.LoadPlannerId;
import be.kdg.airfreight.loadplanning.infrastructure.floorplan.Boeing747400FFloorPlan;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestClock.class)
class DbFlightCargoRepositoryTest {
    private static final FloorPlan FLOOR_PLAN = Boeing747400FFloorPlan.build();

    @Autowired
    private FlightCargoRepository cargos;
    @Autowired
    private LoadingPolicy policy;
    @Autowired
    private TestHelper testHelper;

    @AfterEach
    void cleanUp() {
        testHelper.cleanUp();
    }

    /**
     * Two load planners read the same cargo and each put another ULD on CL. Each copy is valid on its own; the
     * version of the aggregate is what stops the second one, so no two ULDs end up on one load position.
     */
    @Test
    void aCargoThatSomeoneElseChangedInTheMeantimeIsNotSaved() {
        FlightCargoId id = testHelper.saveCargo().getId();
        testHelper.book(id, "AKE16689R7", "1250", WeightUnit.KG, "ATH");
        testHelper.book(id, "AKE20417HJ", "2646", WeightUnit.LB, "FRA");
        LoadPlannerId planner = testHelper.saveLoadPlanner("Test load planner");
        FlightCargo first = cargos.findById(id).orElseThrow();
        FlightCargo second = cargos.findById(id).orElseThrow();

        first.assign(new UldCode("AKE16689R7"), FLOOR_PLAN.position("CL"), planner,
            TestHelper.IN_THE_LOADING_PERIOD, FLOOR_PLAN, policy);
        cargos.save(first);
        second.assign(new UldCode("AKE20417HJ"), FLOOR_PLAN.position("CL"), planner,
            TestHelper.IN_THE_LOADING_PERIOD, FLOOR_PLAN, policy);

        assertThatThrownBy(() -> cargos.save(second)).isInstanceOf(OptimisticLockingFailureException.class);
        assertThat(cargos.findById(id).orElseThrow().getLoad()).extracting(Uld::getCode)
            .containsExactly(new UldCode("AKE16689R7"));
    }

    /** Seven characters (a three-letter airline code and four digits) is the longest flight number. */
    @Test
    void aCargoWithAFlightNumberOfSevenCharactersIsSavedAndFoundByItsFlight() {
        FlightCargo cargo = testHelper.saveCargo("ABC1234", TestHelper.DATE, LocalTime.of(6, 30));

        assertThat(cargos.findByFlightId(cargo.getFlightId())).map(FlightCargo::getId).contains(cargo.getId());
    }
}
