package be.kdg.airfreight.loadplanning.domain.flightcargo;

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
 * What FlightCargo returns. The Cucumber scenarios check the state of FlightCargo; these tests check the events
 * that the application service publishes (the CPM comes from LoadFinalized) and the answer of hasBooked.
 */
class FlightCargoTest {
    private static final Instant NOW = Instant.parse("2026-01-09T05:00:00Z");
    private static final LoadingPolicy POLICY = new LoadingPolicy(Duration.ofHours(12), Duration.ofMinutes(20));
    private static final UldCode CODE = new UldCode("AKE16689R7");
    private static final Weight WEIGHT = new Weight(new BigDecimal("1250"), WeightUnit.KG);
    private static final AirportCode ATH = new AirportCode("ATH");
    private static final LoadPosition CL = new LoadPosition("CL", Side.LEFT);
    private static final FloorPlan FLOOR_PLAN = new FloorPlan(new AircraftType("TEST"), Map.of(
        CL, List.of(new Route(List.of()))));

    private final LoadPlannerId planner = LoadPlannerId.create();
    private final FlightCargo cargo = FlightCargo.create(
        new FlightId(new FlightNumber("HJ221"), LocalDate.of(2026, Month.JANUARY, 9), new AirportCode("BRU")),
        FlightInfo.scheduled(ATH, new AircraftType("TEST"),
            Instant.parse("2026-01-09T06:45:00Z"), Instant.parse("2026-01-09T10:00:00Z")), POLICY);

    @Test
    void bookingAnnouncesWhatWasBooked() {
        UldBooked booked = cargo.book(CODE, WEIGHT, ATH, NOW);

        assertThat(booked).isEqualTo(new UldBooked(cargo.getId(), cargo.getFlightId(), CODE, WEIGHT, ATH, NOW));
        assertThat(cargo.hasBooked(CODE)).isTrue();
    }

    @Test
    void aCodeThatIsNotBookedIsNotBooked() {
        cargo.book(CODE, WEIGHT, ATH, NOW);

        assertThat(cargo.hasBooked(new UldCode("AKE20417HJ"))).isFalse();
    }

    @Test
    void assigningAnnouncesThePositionAndWhoAssignedIt() {
        cargo.book(CODE, WEIGHT, ATH, NOW);

        LoadPositionAssigned assigned = cargo.assign(CODE, CL, planner, NOW, FLOOR_PLAN, POLICY);

        assertThat(assigned).isEqualTo(
            new LoadPositionAssigned(cargo.getId(), cargo.getFlightId(), CODE, CL, planner, NOW));
    }

    @Test
    void clearingAnnouncesTheOldPositionAndWhoClearedIt() {
        cargo.book(CODE, WEIGHT, ATH, NOW);
        cargo.assign(CODE, CL, planner, NOW, FLOOR_PLAN, POLICY);

        LoadPositionCleared cleared = cargo.clear(CODE, planner, NOW, POLICY);

        assertThat(cleared).isEqualTo(
            new LoadPositionCleared(cargo.getId(), cargo.getFlightId(), CODE, CL, planner, NOW));
    }

    /** Only the ULDs with a load position are in the load: the CPM lists what is on board. */
    @Test
    void finalizingAnnouncesTheLoad() {
        cargo.book(CODE, WEIGHT, ATH, NOW);
        cargo.assign(CODE, CL, planner, NOW, FLOOR_PLAN, POLICY);
        cargo.book(new UldCode("AKE20417HJ"), WEIGHT, ATH, NOW);

        LoadFinalized finalized = cargo.finalizeLoad(planner, NOW, List.of(), FLOOR_PLAN, POLICY);

        assertThat(finalized).isEqualTo(new LoadFinalized(cargo.getId(), cargo.getFlightId(),
            List.of(new LoadedUld(CODE, CL, WEIGHT, ATH)), planner, NOW));
    }
}
