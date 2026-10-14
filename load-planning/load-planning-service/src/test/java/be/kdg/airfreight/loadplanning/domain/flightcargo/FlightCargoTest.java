package be.kdg.airfreight.loadplanning.domain.flightcargo;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import be.kdg.airfreight.loadplanning.domain.loadplanner.LoadPlannerId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** What the cargo says about itself. The edges of the loading period are the Examples of story 18. */
class FlightCargoTest {
    private static final LoadingPolicy POLICY = new LoadingPolicy(Duration.ofHours(12), Duration.ofMinutes(20));
    private static final AirportCode ATH = new AirportCode("ATH");
    private static final LoadPosition CL = new LoadPosition("CL", Side.LEFT);
    private static final FloorPlan FLOOR_PLAN = new FloorPlan(new AircraftType("TEST"), floorPlanOfCL());
    private static final Instant IN_THE_LOADING_PERIOD = Instant.parse("2026-01-09T05:00:00Z");

    private final LoadPlannerId planner = LoadPlannerId.create();
    private final FlightCargo cargo = FlightCargo.create(
        new FlightId(new FlightNumber("HJ221"), LocalDate.of(2026, Month.JANUARY, 9), new AirportCode("BRU")),
        FlightInfo.scheduled(ATH, new AircraftType("TEST"),
            Instant.parse("2026-01-09T06:30:00Z"), Instant.parse("2026-01-09T09:45:00Z")), POLICY);

    @Test
    void theLoadCanChangeUntilJustBeforeTheEndOfTheLoadingPeriod() {
        assertThat(cargo.loadCanChange(Instant.parse("2026-01-09T06:09:00Z"), POLICY)).isTrue();
        assertThat(cargo.loadCanChange(Instant.parse("2026-01-09T06:10:00Z"), POLICY)).isFalse();
    }

    @Test
    void aFinalizedLoadCannotChange() {
        cargo.book(new UldCode("AKE16689R7"), new Weight(new BigDecimal("1250"), WeightUnit.KG), ATH,
            IN_THE_LOADING_PERIOD);
        cargo.assign(new UldCode("AKE16689R7"), CL, planner, IN_THE_LOADING_PERIOD, FLOOR_PLAN, POLICY);
        cargo.finalizeLoad(planner, IN_THE_LOADING_PERIOD, List.of(), FLOOR_PLAN, POLICY);

        assertThat(cargo.loadCanChange(IN_THE_LOADING_PERIOD, POLICY)).isFalse();
    }

    @Test
    void theLoadOfADepartedFlightCannotChange() {
        cargo.updateFlightInfo(cargo.getFlightInfo().asDeparted());

        assertThat(cargo.loadCanChange(IN_THE_LOADING_PERIOD, POLICY)).isFalse();
    }

    @Test
    void aFlightIsToBeLoadedUntilItHasDeparted() {
        assertThat(cargo.isToBeLoaded()).isTrue();

        cargo.finalizeLoad(planner, IN_THE_LOADING_PERIOD, List.of(), FLOOR_PLAN, POLICY);
        assertThat(cargo.isToBeLoaded()).as("a finalized load that has not departed").isTrue();

        cargo.updateFlightInfo(cargo.getFlightInfo().asDeparted());
        assertThat(cargo.isToBeLoaded()).isFalse();
    }

    private static Map<LoadPosition, List<Route>> floorPlanOfCL() {
        Map<LoadPosition, List<Route>> routes = new LinkedHashMap<>();
        routes.put(CL, List.of(new Route(List.of())));
        return routes;
    }
}
