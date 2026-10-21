package be.kdg.airfreight.loadplanning.api.flightcargo;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import be.kdg.airfreight.loadplanning.application.FlightCargoSnapshot;
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

class FlightCargoDtoTest {
    private static final Instant NOW = Instant.parse("2026-01-09T05:00:00Z");
    private static final LoadingPolicy POLICY = new LoadingPolicy(Duration.ofHours(12), Duration.ofMinutes(20));
    private static final LoadPosition CL = new LoadPosition("CL", Side.LEFT);
    private static final LoadPosition CR = new LoadPosition("CR", Side.RIGHT);
    private static final LoadPosition A1 = new LoadPosition("A1", null);
    private static final FloorPlan FLOOR_PLAN = new FloorPlan(new AircraftType("TEST"), floorPlanOfA1ClAndCr(), CL);

    private final FlightCargo cargo = FlightCargo.create(
        new FlightId(new FlightNumber("HJ221"), LocalDate.of(2026, 1, 9), new AirportCode("BRU")),
        FlightInfo.scheduled(new AirportCode("ATH"), new AircraftType("TEST"),
            Instant.parse("2026-01-09T06:30:00Z"), Instant.parse("2026-01-09T09:45:00Z")), POLICY);

    @Test
    void theCargoIsShownWithItsFlightItsUldsInOrderOfCodeAndTheLoadPositionsOfTheFloorPlan() {
        cargo.book(new UldCode("AKE20417HJ"), new Weight(new BigDecimal("2646"), WeightUnit.LB),
            new AirportCode("FRA"), NOW);
        cargo.book(new UldCode("AKE16689R7"), new Weight(new BigDecimal("1250"), WeightUnit.KG),
            new AirportCode("ATH"), NOW);
        LoadPlannerId lotte = LoadPlannerId.create();
        cargo.assign(new UldCode("AKE16689R7"), CL, lotte, NOW, FLOOR_PLAN, POLICY);

        FlightCargoDto dto = FlightCargoDto.from(view(), Map.of(lotte, "Lotte"));

        assertThat(dto.id()).isEqualTo(cargo.getId().id());
        assertThat(dto.flightNumber()).isEqualTo("HJ221");
        assertThat(dto.scheduledDepartureDate()).isEqualTo(LocalDate.of(2026, 1, 9));
        assertThat(dto.departureAirport()).isEqualTo("BRU");
        assertThat(dto.arrivalAirport()).isEqualTo("ATH");
        assertThat(dto.aircraftType()).isEqualTo("TEST");
        assertThat(dto.std()).isEqualTo(Instant.parse("2026-01-09T06:30:00Z"));
        assertThat(dto.sta()).isEqualTo(Instant.parse("2026-01-09T09:45:00Z"));
        assertThat(dto.etd()).isEqualTo(dto.std());
        assertThat(dto.eta()).isEqualTo(dto.sta());
        assertThat(dto.departed()).isFalse();
        assertThat(dto.finalized()).isFalse();
        assertThat(dto.finalizedBy()).isNull();
        assertThat(dto.loadingPeriod()).isEqualTo(new LoadingPeriodDto(
            Instant.parse("2026-01-08T18:30:00Z"), Instant.parse("2026-01-09T06:10:00Z")));
        assertThat(dto.loadCanChange()).isTrue();
        assertThat(dto.ulds()).containsExactly(
            new UldDto("AKE16689R7", new WeightDto(new BigDecimal("1250"), WeightUnit.KG), "ATH", "CL", "Lotte"),
            new UldDto("AKE20417HJ", new WeightDto(new BigDecimal("2646"), WeightUnit.LB), "FRA", null, null));
        assertThat(dto.loadPositions()).containsExactly("A1", "CL", "CR");
    }

    @Test
    void aUldWithoutALoadPositionHasNone() {
        cargo.book(new UldCode("AKE20417HJ"), new Weight(new BigDecimal("2646"), WeightUnit.LB),
            new AirportCode("FRA"), NOW);

        assertThat(FlightCargoDto.from(view(), Map.of()).ulds()).extracting(UldDto::loadPosition).containsOnlyNulls();
    }

    private FlightCargoSnapshot view() {
        return new FlightCargoSnapshot(cargo, FLOOR_PLAN, cargo.loadingPeriod(POLICY),
            cargo.loadCanChange(NOW, POLICY));
    }

    private static Map<LoadPosition, List<Route>> floorPlanOfA1ClAndCr() {
        Map<LoadPosition, List<Route>> routes = new LinkedHashMap<>();
        routes.put(A1, List.of(new Route(List.of(CL, CR))));
        routes.put(CL, List.of(new Route(List.of())));
        routes.put(CR, List.of(new Route(List.of(CL))));
        return routes;
    }
}
