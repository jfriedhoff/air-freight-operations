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
import be.kdg.airfreight.loadplanning.domain.flightcargo.LateralBalanceDetails;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LateralBalanceLimit;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadCheckResult;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadPosition;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadingPolicy;
import be.kdg.airfreight.loadplanning.domain.flightcargo.Route;
import be.kdg.airfreight.loadplanning.domain.flightcargo.Side;
import be.kdg.airfreight.loadplanning.domain.flightcargo.UldCode;
import be.kdg.airfreight.loadplanning.domain.flightcargo.Weight;
import be.kdg.airfreight.loadplanning.domain.flightcargo.WeightUnit;
import be.kdg.airfreight.loadplanning.domain.loadplanner.LoadPlannerId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** What the lateral balance check reports about both sides, on a deck of CL, CR and A1 in the nose; limit 10%. */
class LateralBalanceCheckTest {
    private static final LoadingPolicy POLICY = new LoadingPolicy(Duration.ofHours(12), Duration.ofMinutes(20));
    private static final Instant NOW = Instant.parse("2026-01-09T05:00:00Z");
    private static final LoadPosition A1 = new LoadPosition("A1", null);
    private static final LoadPosition CL = new LoadPosition("CL", Side.LEFT);
    private static final LoadPosition CR = new LoadPosition("CR", Side.RIGHT);
    private static final FloorPlan FLOOR_PLAN = new FloorPlan(new AircraftType("TEST"), routes(), CL);

    private final LateralBalanceCheck check = new LateralBalanceCheck(new LateralBalanceLimit(new BigDecimal("0.10")));
    private final LoadPlannerId planner = LoadPlannerId.create();
    private final FlightCargo cargo = FlightCargo.create(
        new FlightId(new FlightNumber("HJ221"), LocalDate.of(2026, 1, 9), new AirportCode("BRU")),
        FlightInfo.scheduled(new AirportCode("ATH"), new AircraftType("TEST"),
            Instant.parse("2026-01-09T06:30:00Z"), Instant.parse("2026-01-09T09:45:00Z")), POLICY);

    @Test
    void bothSidesTheMaximumAndTheHeavierSideAreReportedWhenTheCheckPasses() {
        place("AKE16689R7", "1000", WeightUnit.KG, CL);
        place("AKE20417HJ", "1100", WeightUnit.KG, CR);

        LoadCheckResult result = check.check(cargo, FLOOR_PLAN);

        assertThat(result.passed()).isTrue();
        LateralBalanceDetails details = details(result);
        assertThat(details.leftKilograms()).isEqualByComparingTo("1000");
        assertThat(details.rightKilograms()).isEqualByComparingTo("1100");
        assertThat(details.maximumOnTheHeaviestSide()).isEqualByComparingTo("1100");
        assertThat(details.heavierSide()).isEqualTo(Side.RIGHT);
    }

    @Test
    void bothSidesAndTheMaximumAreReportedWhenTheCheckFails() {
        place("AKE16689R7", "1000", WeightUnit.KG, CL);
        place("AKE20417HJ", "1101", WeightUnit.KG, CR);

        LoadCheckResult result = check.check(cargo, FLOOR_PLAN);

        assertThat(result.passed()).isFalse();
        assertThat(result.reason())
            .isEqualTo("Left carries 1000 kg and right 1101 kg; the heaviest side may carry at most 1100 kg");
        LateralBalanceDetails details = details(result);
        assertThat(details.rightKilograms()).isEqualByComparingTo("1101");
        assertThat(details.maximumOnTheHeaviestSide()).isEqualByComparingTo("1100");
        assertThat(details.heavierSide()).isEqualTo(Side.RIGHT);
    }

    @Test
    void poundsAreConvertedToKilograms() {
        place("PMC12345KL", "2646", WeightUnit.LB, CL);

        assertThat(details(check.check(cargo, FLOOR_PLAN)).leftKilograms())
            .isEqualByComparingTo(new Weight(new BigDecimal("2646"), WeightUnit.LB).toKilograms());
    }

    @Test
    void aPositionWithoutASideCountsForNeitherAndEqualSidesHaveNoHeavierSide() {
        place("AKE16689R7", "1000", WeightUnit.KG, A1);

        LateralBalanceDetails details = details(check.check(cargo, FLOOR_PLAN));

        assertThat(details.leftKilograms()).isEqualByComparingTo("0");
        assertThat(details.rightKilograms()).isEqualByComparingTo("0");
        assertThat(details.heavierSide()).isNull();
    }

    private void place(String code, String weight, WeightUnit unit, LoadPosition position) {
        UldCode uld = new UldCode(code);
        cargo.book(uld, new Weight(new BigDecimal(weight), unit), new AirportCode("ATH"), NOW);
        cargo.assign(uld, position, planner, NOW, FLOOR_PLAN, POLICY);
    }

    private static LateralBalanceDetails details(LoadCheckResult result) {
        assertThat(result.details()).isInstanceOf(LateralBalanceDetails.class);
        return (LateralBalanceDetails) result.details();
    }

    private static Map<LoadPosition, List<Route>> routes() {
        Map<LoadPosition, List<Route>> routes = new LinkedHashMap<>();
        routes.put(A1, List.of(new Route(List.of(CL, CR))));
        routes.put(CL, List.of(new Route(List.of())));
        routes.put(CR, List.of(new Route(List.of(CL))));
        return routes;
    }
}
