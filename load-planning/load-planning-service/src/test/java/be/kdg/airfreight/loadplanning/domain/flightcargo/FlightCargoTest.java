package be.kdg.airfreight.loadplanning.domain.flightcargo;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import be.kdg.airfreight.loadplanning.domain.DomainRuleViolationException;
import be.kdg.airfreight.loadplanning.domain.NotFoundException;
import be.kdg.airfreight.loadplanning.domain.loadplanner.LoadPlannerId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * What FlightCargo returns. The Cucumber scenarios check its state; these tests check what the cargo says about
 * itself and the events that the application service publishes (the CPM comes from LoadFinalized). The edges of
 * the loading period are the Examples of story 18.
 */
class FlightCargoTest {
    private static final LoadingPolicy POLICY = new LoadingPolicy(Duration.ofHours(12), Duration.ofMinutes(20));
    private static final AirportCode ATH = new AirportCode("ATH");
    private static final LoadPosition CL = new LoadPosition("CL", Side.LEFT);
    private static final FloorPlan FLOOR_PLAN = new FloorPlan(new AircraftType("TEST"), floorPlanOfCL());
    private static final Instant IN_THE_LOADING_PERIOD = Instant.parse("2026-01-09T05:00:00Z");
    private static final UldCode CODE = new UldCode("AKE16689R7");
    private static final Weight WEIGHT = new Weight(new BigDecimal("1250"), WeightUnit.KG);

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
        cargo.book(CODE, WEIGHT, ATH, IN_THE_LOADING_PERIOD);
        cargo.assign(CODE, CL, planner, IN_THE_LOADING_PERIOD, FLOOR_PLAN, POLICY);
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

    @Test
    void bookingAnnouncesWhatWasBooked() {
        UldBooked booked = cargo.book(CODE, WEIGHT, ATH, IN_THE_LOADING_PERIOD);

        assertThat(booked).isEqualTo(
            new UldBooked(cargo.getId(), cargo.getFlightId(), CODE, WEIGHT, ATH, IN_THE_LOADING_PERIOD));
        assertThat(cargo.hasBooked(CODE)).isTrue();
    }

    @Test
    void aCodeThatIsNotBookedIsNotBooked() {
        cargo.book(CODE, WEIGHT, ATH, IN_THE_LOADING_PERIOD);

        assertThat(cargo.hasBooked(new UldCode("AKE20417HJ"))).isFalse();
    }

    @Test
    void assigningAnnouncesThePositionAndWhoAssignedIt() {
        cargo.book(CODE, WEIGHT, ATH, IN_THE_LOADING_PERIOD);

        LoadPositionAssigned assigned = cargo.assign(CODE, CL, planner, IN_THE_LOADING_PERIOD, FLOOR_PLAN, POLICY);

        assertThat(assigned).isEqualTo(new LoadPositionAssigned(cargo.getId(), cargo.getFlightId(), CODE, CL, planner,
            IN_THE_LOADING_PERIOD));
    }

    @Test
    void clearingAnnouncesTheOldPositionAndWhoClearedIt() {
        cargo.book(CODE, WEIGHT, ATH, IN_THE_LOADING_PERIOD);
        cargo.assign(CODE, CL, planner, IN_THE_LOADING_PERIOD, FLOOR_PLAN, POLICY);

        LoadPositionCleared cleared = cargo.clear(CODE, planner, IN_THE_LOADING_PERIOD, POLICY);

        assertThat(cleared).isEqualTo(new LoadPositionCleared(cargo.getId(), cargo.getFlightId(), CODE, CL, planner,
            IN_THE_LOADING_PERIOD));
    }

    /** Only the ULDs with a load position are in the load: the CPM lists what is on board. */
    @Test
    void placingAUldOnThePositionItAlreadyHasIsAccepted() {
        cargo.book(CODE, WEIGHT, ATH, IN_THE_LOADING_PERIOD);
        cargo.assign(CODE, CL, planner, IN_THE_LOADING_PERIOD, FLOOR_PLAN, POLICY);

        cargo.assign(CODE, CL, planner, IN_THE_LOADING_PERIOD, FLOOR_PLAN, POLICY);

        assertThat(cargo.getLoad()).extracting(Uld::getPosition).containsExactly(CL);
    }

    @Test
    void clearingAUldWithoutALoadPositionIsRefused() {
        cargo.book(CODE, WEIGHT, ATH, IN_THE_LOADING_PERIOD);

        assertThatThrownBy(() -> cargo.clear(CODE, planner, IN_THE_LOADING_PERIOD, POLICY))
            .isInstanceOf(DomainRuleViolationException.class)
            .hasMessage("ULD AKE16689R7 has no load position");
    }

    @Test
    void theLoadOfADepartedFlightIsNotChanged() {
        cargo.book(CODE, WEIGHT, ATH, IN_THE_LOADING_PERIOD);
        cargo.updateFlightInfo(cargo.getFlightInfo().asDeparted());

        assertThatThrownBy(() -> cargo.assign(CODE, CL, planner, IN_THE_LOADING_PERIOD, FLOOR_PLAN, POLICY))
            .isInstanceOf(DomainRuleViolationException.class)
            .hasMessage("The load can no longer change: the flight has departed");
        assertThat(cargo.getLoad()).isEmpty();
    }

    @Test
    void aUldThatIsNotBookedOnTheFlightIsNotFound() {
        assertThatThrownBy(() -> cargo.assign(CODE, CL, planner, IN_THE_LOADING_PERIOD, FLOOR_PLAN, POLICY))
            .isInstanceOf(NotFoundException.class)
            .hasMessage("ULD [AKE16689R7] is not booked on this flight");
        assertThatThrownBy(() -> cargo.clear(CODE, planner, IN_THE_LOADING_PERIOD, POLICY))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void finalizingAnnouncesTheLoad() {
        cargo.book(CODE, WEIGHT, ATH, IN_THE_LOADING_PERIOD);
        cargo.assign(CODE, CL, planner, IN_THE_LOADING_PERIOD, FLOOR_PLAN, POLICY);
        cargo.book(new UldCode("AKE20417HJ"), WEIGHT, ATH, IN_THE_LOADING_PERIOD);

        LoadFinalized finalized = cargo.finalizeLoad(planner, IN_THE_LOADING_PERIOD, List.of(), FLOOR_PLAN, POLICY);

        assertThat(finalized).isEqualTo(new LoadFinalized(cargo.getId(), cargo.getFlightId(),
            List.of(new LoadedUld(CODE, CL, WEIGHT, ATH)), planner, IN_THE_LOADING_PERIOD));
    }

    private static Map<LoadPosition, List<Route>> floorPlanOfCL() {
        Map<LoadPosition, List<Route>> routes = new LinkedHashMap<>();
        routes.put(CL, List.of(new Route(List.of())));
        return routes;
    }
}
