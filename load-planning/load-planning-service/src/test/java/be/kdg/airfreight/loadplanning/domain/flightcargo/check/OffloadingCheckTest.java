package be.kdg.airfreight.loadplanning.domain.flightcargo.check;

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

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The report of a failed offloading check when more than one ULD is involved. The stories 20 and 21 only check
 * the report for one ULD in the way, so these cases have no scenario.
 */
class OffloadingCheckTest {
    private static final Instant NOW = Instant.parse("2026-01-09T05:00:00Z");
    private static final LoadingPolicy POLICY = new LoadingPolicy(Duration.ofHours(12), Duration.ofMinutes(20));
    private static final AirportCode ATH = new AirportCode("ATH");
    private static final AirportCode FRA = new AirportCode("FRA");
    private static final LoadPosition A1 = new LoadPosition("A1", null);
    private static final LoadPosition A2 = new LoadPosition("A2", null);
    private static final LoadPosition CL = new LoadPosition("CL", Side.LEFT);
    private static final LoadPosition CR = new LoadPosition("CR", Side.RIGHT);
    private static final LoadPosition DL = new LoadPosition("DL", Side.LEFT);
    /** A small floor plan of its own: A1 and A2 must pass CL, CR and DL; those three lead straight to the door. */
    private static final FloorPlan FLOOR_PLAN = new FloorPlan(new AircraftType("TEST"), Map.of(
        A1, List.of(new Route(List.of(CL, CR, DL))),
        A2, List.of(new Route(List.of(CL, CR, DL))),
        CL, List.of(new Route(List.of())),
        CR, List.of(new Route(List.of())),
        DL, List.of(new Route(List.of()))));

    private final LoadPlannerId planner = LoadPlannerId.create();
    private final FlightCargo cargo = FlightCargo.create(
        new FlightId(new FlightNumber("HJ221"), LocalDate.of(2026, Month.JANUARY, 9), new AirportCode("BRU")),
        FlightInfo.scheduled(ATH, new AircraftType("TEST"),
            Instant.parse("2026-01-09T06:45:00Z"), Instant.parse("2026-01-09T10:00:00Z")), POLICY);

    @Test
    void theReportNamesEveryUldInTheWay() {
        load("AKE00001HJ", ATH, A1);
        load("AKE00004HJ", FRA, DL);
        load("AKE00002HJ", FRA, CL);
        load("AKE00003HJ", FRA, CR);

        assertThat(new OffloadingCheck().check(cargo, FLOOR_PLAN).reason()).isEqualTo(
            "AKE00001HJ on A1 cannot be offloaded because AKE00002HJ on CL, AKE00003HJ on CR and AKE00004HJ on DL"
                + " are in the way");
    }

    @Test
    void theReportNamesEveryUldThatCannotBeOffloaded() {
        load("AKE00002HJ", ATH, A1);
        load("AKE00001HJ", ATH, A2);
        load("AKE00003HJ", FRA, CL);

        assertThat(new OffloadingCheck().check(cargo, FLOOR_PLAN).reason()).isEqualTo(
            "AKE00001HJ on A2 cannot be offloaded because AKE00003HJ on CL is in the way; "
                + "AKE00002HJ on A1 cannot be offloaded because AKE00003HJ on CL is in the way");
    }

    private void load(String code, AirportCode destination, LoadPosition position) {
        cargo.book(new UldCode(code), new Weight(new BigDecimal("1000"), WeightUnit.KG), destination, NOW);
        cargo.assign(new UldCode(code), position, planner, NOW, FLOOR_PLAN, POLICY);
    }
}
