package be.kdg.airfreight.loadplanning.acceptance.unit;

import be.kdg.airfreight.loadplanning.domain.DomainRuleViolationException;
import be.kdg.airfreight.loadplanning.domain.flightcargo.AirportCode;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargo;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightId;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightInfo;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightNumber;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FloorPlan;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LateralBalanceLimit;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadCheck;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadCheckReport;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadCheckResult;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadingPolicy;
import be.kdg.airfreight.loadplanning.domain.flightcargo.Uld;
import be.kdg.airfreight.loadplanning.domain.flightcargo.UldCode;
import be.kdg.airfreight.loadplanning.domain.flightcargo.Weight;
import be.kdg.airfreight.loadplanning.domain.flightcargo.WeightUnit;
import be.kdg.airfreight.loadplanning.domain.flightcargo.check.AllCargoLoadedCheck;
import be.kdg.airfreight.loadplanning.domain.flightcargo.check.LateralBalanceCheck;
import be.kdg.airfreight.loadplanning.domain.flightcargo.check.OffloadingCheck;
import be.kdg.airfreight.loadplanning.domain.loadplanner.LoadPlannerId;
import be.kdg.airfreight.loadplanning.infrastructure.floorplan.Boeing747400FFloorPlan;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Locale;

/**
 * The state of one scenario, shared by all step classes. Picocontainer creates a new one for every scenario,
 * so nothing leaks from one scenario into the next. Never make these fields static.
 */
public class CargoWorld {
    static final Instant NOW = Instant.parse("2026-01-09T05:00:00Z");
    static final LoadingPolicy POLICY = new LoadingPolicy(Duration.ofHours(12), Duration.ofMinutes(20));
    static final LateralBalanceLimit LATERAL_BALANCE_LIMIT = new LateralBalanceLimit(new BigDecimal("0.10"));
    /** All load checks, as PolicyConfig gives them to the application service. */
    static final List<LoadCheck> CHECKS = List.of(
        new LateralBalanceCheck(LATERAL_BALANCE_LIMIT), new OffloadingCheck(), new AllCargoLoadedCheck());
    private static final AirportCode ATH = new AirportCode("ATH");

    final FloorPlan floorPlan = Boeing747400FFloorPlan.build();
    final FlightCargo cargo = FlightCargo.create(
        new FlightId(new FlightNumber("HJ221"), LocalDate.of(2026, Month.JANUARY, 9), new AirportCode("BRU")),
        FlightInfo.scheduled(ATH, Boeing747400FFloorPlan.AIRCRAFT_TYPE,
            Instant.parse("2026-01-09T06:45:00Z"), Instant.parse("2026-01-09T10:00:00Z")));
    final LoadPlannerId planner = LoadPlannerId.create();
    /** The moment the scenario takes place; the loading period of the flight is open at NOW. */
    Instant now = NOW;
    LoadCheckResult result;
    LoadCheckReport report;
    /** What the last When step was refused with, or null when it was accepted. */
    DomainRuleViolationException error;
    /** Set by the When steps: a step that is both a Given and a Then sets up before it and checks after it. */
    boolean acted;
    private int uldCount;

    /** The flight as it is, only with another destination. */
    void goesTo(String airport) {
        FlightInfo info = cargo.getFlightInfo();
        cargo.updateFlightInfo(new FlightInfo(new AirportCode(airport), info.aircraftType(), info.std(), info.sta(),
            info.etd(), info.eta(), info.departed()));
    }

    /** The flight with another STD. Without delays the ETD is the STD. */
    void departsAt(Instant std) {
        FlightInfo info = cargo.getFlightInfo();
        cargo.updateFlightInfo(new FlightInfo(info.arrivalAirport(), info.aircraftType(), std, info.sta(), std,
            info.eta(), info.departed()));
    }

    /** The ETD becomes the STD plus the departure delays. */
    void delayDepartureBy(Duration delays) {
        FlightInfo info = cargo.getFlightInfo();
        cargo.updateFlightInfo(info.withEstimatedDeparture(info.std().plus(delays)));
    }

    /** Books a new ULD for ATH and puts it on the position. "0 kg" loads nothing: a ULD always weighs something. */
    void load(String position, String weight) {
        if (new BigDecimal(weight.split(" ")[0]).signum() == 0) {
            return;
        }
        UldCode code = nextCode();
        book(code, weight);
        place(code, position);
    }

    void book(UldCode code, String weight) {
        book(code, weight, ATH.value());
    }

    void book(UldCode code, String weight, String destination) {
        cargo.book(code, weightOf(weight), new AirportCode(destination), now);
    }

    void place(UldCode code, String position) {
        cargo.assign(code, floorPlan.positionNamed(position).orElseThrow(), planner, now, floorPlan, POLICY);
    }

    Uld uld(String code) {
        return cargo.getUlds().stream()
            .filter(uld -> uld.getCode().equals(new UldCode(code)))
            .findFirst()
            .orElseThrow();
    }

    /** A code for a ULD the scenario does not name: AKE00001HJ, AKE00002HJ, ... */
    UldCode nextCode() {
        return new UldCode("AKE%05dHJ".formatted(++uldCount));
    }

    void check(LoadCheck check) {
        result = cargo.checkLoad(List.of(check), floorPlan).results().getFirst();
    }

    void checkAll() {
        report = cargo.checkLoad(CHECKS, floorPlan);
    }

    /** "2646 lb" becomes a Weight of 2646 LB. */
    private static Weight weightOf(String weight) {
        String[] parts = weight.split(" ");
        return new Weight(new BigDecimal(parts[0]), WeightUnit.valueOf(parts[1].toUpperCase(Locale.ROOT)));
    }
}
