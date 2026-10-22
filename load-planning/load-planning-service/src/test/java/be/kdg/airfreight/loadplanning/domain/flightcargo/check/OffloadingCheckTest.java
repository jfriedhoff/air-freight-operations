package be.kdg.airfreight.loadplanning.domain.flightcargo.check;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import be.kdg.airfreight.loadplanning.domain.flightcargo.AircraftType;
import be.kdg.airfreight.loadplanning.domain.flightcargo.AirportCode;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargo;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightId;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightInfo;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightNumber;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FloorPlan;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadCheckResult;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadPosition;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadedUld;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadingPolicy;
import be.kdg.airfreight.loadplanning.domain.flightcargo.Obstruction;
import be.kdg.airfreight.loadplanning.domain.flightcargo.OffloadingDetails;
import be.kdg.airfreight.loadplanning.domain.flightcargo.Route;
import be.kdg.airfreight.loadplanning.domain.flightcargo.Side;
import be.kdg.airfreight.loadplanning.domain.flightcargo.UldCode;
import be.kdg.airfreight.loadplanning.domain.flightcargo.Weight;
import be.kdg.airfreight.loadplanning.domain.flightcargo.WeightUnit;
import be.kdg.airfreight.loadplanning.domain.loadplanner.LoadPlannerId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What the offloading check reports, on a small deck of one side: ML and PL lead to the door at RL, and A1 in the
 * nose needs both CL and CR free before it goes left. The flight goes to ATH.
 */
class OffloadingCheckTest {
    private static final LoadingPolicy POLICY = new LoadingPolicy(Duration.ofHours(12), Duration.ofMinutes(20));
    private static final Instant NOW = Instant.parse("2026-01-09T05:00:00Z");
    private static final AirportCode ATH = new AirportCode("ATH");
    private static final LoadPosition A1 = new LoadPosition("A1", null);
    private static final LoadPosition CL = new LoadPosition("CL", Side.LEFT);
    private static final LoadPosition CR = new LoadPosition("CR", Side.RIGHT);
    private static final LoadPosition ML = new LoadPosition("ML", Side.LEFT);
    private static final LoadPosition PL = new LoadPosition("PL", Side.LEFT);
    private static final LoadPosition RL = new LoadPosition("RL", Side.LEFT);
    private static final FloorPlan FLOOR_PLAN = new FloorPlan(new AircraftType("TEST"), routes(), RL);

    private final OffloadingCheck check = new OffloadingCheck();
    private final LoadPlannerId planner = LoadPlannerId.create();
    private final FlightCargo cargo = FlightCargo.create(
        new FlightId(new FlightNumber("HJ221"), LocalDate.of(2026, 1, 9), new AirportCode("BRU")),
        FlightInfo.scheduled(ATH, new AircraftType("TEST"),
            Instant.parse("2026-01-09T06:30:00Z"), Instant.parse("2026-01-09T09:45:00Z")), POLICY);

    @Test
    void aStuckUldNamesTheUldInItsWay() {
        place("AKE16689R7", "ATH", ML);
        place("AKE20417HJ", "FRA", PL);

        LoadCheckResult result = check.check(cargo, FLOOR_PLAN);

        assertThat(result.passed()).isFalse();
        assertThat(result.reason())
            .isEqualTo("AKE16689R7 on ML cannot be offloaded because AKE20417HJ on PL is in the way");
        assertThat(obstructions(result)).containsExactly("AKE16689R7@ML blocked by [AKE20417HJ@PL]");
    }

    @Test
    void twoUldsInTheWayAreBothNamedInTheOrderOfTheSentence() {
        place("AKE16689R7", "ATH", ML);
        place("PAG55501HJ", "LGG", PL);
        place("AKE20417HJ", "FRA", RL);

        LoadCheckResult result = check.check(cargo, FLOOR_PLAN);

        assertThat(result.reason()).isEqualTo(
            "AKE16689R7 on ML cannot be offloaded because AKE20417HJ on RL and PAG55501HJ on PL are in the way");
        assertThat(obstructions(result)).containsExactly("AKE16689R7@ML blocked by [AKE20417HJ@RL, PAG55501HJ@PL]");
    }

    @Test
    void aNosePositionNeedsBothPositionsOfTheFirstRow() {
        place("AKE16689R7", "ATH", A1);
        place("AKE20417HJ", "FRA", CR);

        assertThat(obstructions(check.check(cargo, FLOOR_PLAN)))
            .containsExactly("AKE16689R7@A1 blocked by [AKE20417HJ@CR]");
    }

    @Test
    void aStuckUldIsAlsoInTheWayOfTheUldBehindIt() {
        place("AKE16689R7", "ATH", ML);
        place("AKE40404R7", "ATH", PL);
        place("AKE20417HJ", "FRA", RL);

        assertThat(obstructions(check.check(cargo, FLOOR_PLAN))).containsExactly(
            "AKE16689R7@ML blocked by [AKE20417HJ@RL, AKE40404R7@PL]",
            "AKE40404R7@PL blocked by [AKE20417HJ@RL]");
    }

    @Test
    void aUldForTheDestinationThatCanBeTakenOutFirstIsNotInTheWay() {
        place("AKE16689R7", "ATH", ML);
        place("AKE40404R7", "ATH", PL);

        LoadCheckResult result = check.check(cargo, FLOOR_PLAN);

        assertThat(result.passed()).isTrue();
        assertThat(result.details()).isNull();
    }

    private void place(String code, String destination, LoadPosition position) {
        UldCode uld = new UldCode(code);
        cargo.book(uld, new Weight(new BigDecimal("1000"), WeightUnit.KG), new AirportCode(destination), NOW);
        cargo.assign(uld, position, planner, NOW, FLOOR_PLAN, POLICY);
    }

    /** "AKE16689R7@ML blocked by [AKE20417HJ@PL]" per obstruction, in the order of the report. */
    private static List<String> obstructions(LoadCheckResult result) {
        assertThat(result.details()).isInstanceOf(OffloadingDetails.class);
        return ((OffloadingDetails) result.details()).obstructions().stream()
            .map(OffloadingCheckTest::describe)
            .toList();
    }

    private static String describe(Obstruction obstruction) {
        return at(obstruction.stuck()) + " blocked by "
            + obstruction.inTheWay().stream().map(OffloadingCheckTest::at).toList();
    }

    private static String at(LoadedUld uld) {
        return uld.code().value() + "@" + uld.position().name();
    }

    private static Map<LoadPosition, List<Route>> routes() {
        Map<LoadPosition, List<Route>> routes = new LinkedHashMap<>();
        routes.put(A1, List.of(new Route(List.of(CL, CR, ML, PL, RL))));
        routes.put(CL, List.of(new Route(List.of(ML, PL, RL))));
        routes.put(CR, List.of(new Route(List.of(CL, ML, PL, RL))));
        routes.put(ML, List.of(new Route(List.of(PL, RL))));
        routes.put(PL, List.of(new Route(List.of(RL))));
        routes.put(RL, List.of(new Route(List.of())));
        return routes;
    }
}
