package be.kdg.airfreight.loadplanning.domain;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A value that is wrong in itself is an InvalidValueException (400 at the API); a refusal that depends on the
 * load stays a plain DomainRuleViolationException (409).
 */
class InvalidValueTest {
    private static final Instant NOW = Instant.parse("2026-01-09T05:00:00Z");
    private static final LoadingPolicy POLICY = new LoadingPolicy(Duration.ofHours(12), Duration.ofMinutes(20));
    private static final AirportCode ATH = new AirportCode("ATH");
    private static final LoadPosition CL = new LoadPosition("CL", Side.LEFT);
    private static final LoadPosition CR = new LoadPosition("CR", Side.RIGHT);
    private static final FloorPlan FLOOR_PLAN = new FloorPlan(new AircraftType("TEST"), floorPlanOfCLAndCR(), CL);

    private final LoadPlannerId planner = LoadPlannerId.create();
    private final FlightCargo cargo = FlightCargo.create(
        new FlightId(new FlightNumber("HJ221"), LocalDate.of(2026, Month.JANUARY, 9), new AirportCode("BRU")),
        FlightInfo.scheduled(ATH, new AircraftType("TEST"),
            Instant.parse("2026-01-09T06:30:00Z"), Instant.parse("2026-01-09T09:45:00Z")), POLICY);

    @Test
    void aMalformedUldCodeAirportCodeOrFlightNumberIsAnInvalidValue() {
        assertThatThrownBy(() -> new UldCode("AKE1")).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new AirportCode("Brussels")).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new FlightNumber("hj221")).isInstanceOf(InvalidValueException.class);
    }

    @Test
    void aBlankLoadPositionOrAWeightOfZeroIsAnInvalidValue() {
        assertThatThrownBy(() -> new LoadPosition(" ", null)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new AircraftType(" ")).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new Weight(BigDecimal.ZERO, WeightUnit.KG)).isInstanceOf(InvalidValueException.class);
    }

    @Test
    void aLoadPositionThatIsNotOnTheFloorPlanIsAnInvalidValue() {
        book("AKE16689R7");
        String reason = "Load position Z9 is not on the floor plan of aircraft type TEST";

        assertThatThrownBy(() -> FLOOR_PLAN.position("Z9"))
            .isInstanceOf(InvalidValueException.class).hasMessage(reason);
        assertThatThrownBy(() -> cargo.assign(new UldCode("AKE16689R7"), new LoadPosition("Z9", null), planner, NOW,
            FLOOR_PLAN, POLICY))
            .isInstanceOf(InvalidValueException.class).hasMessage(reason);
    }

    @Test
    void aPositionOnTheFloorPlanIsFoundByItsName() {
        assertThat(FLOOR_PLAN.position("CR")).isEqualTo(CR);
    }

    @Test
    void anOccupiedLoadPositionDependsOnTheLoadAndIsNotAnInvalidValue() {
        book("AKE16689R7");
        book("AKE20417HJ");
        cargo.assign(new UldCode("AKE16689R7"), CL, planner, NOW, FLOOR_PLAN, POLICY);

        assertThatThrownBy(() -> cargo.assign(new UldCode("AKE20417HJ"), CL, planner, NOW, FLOOR_PLAN, POLICY))
            .isExactlyInstanceOf(DomainRuleViolationException.class)
            .hasMessage("Load position CL already holds ULD AKE16689R7");
    }

    private void book(String code) {
        cargo.book(new UldCode(code), new Weight(new BigDecimal("1250"), WeightUnit.KG), ATH, NOW);
    }

    private static Map<LoadPosition, List<Route>> floorPlanOfCLAndCR() {
        Map<LoadPosition, List<Route>> routes = new LinkedHashMap<>();
        routes.put(CL, List.of(new Route(List.of())));
        routes.put(CR, List.of(new Route(List.of(CL))));
        return routes;
    }
}
