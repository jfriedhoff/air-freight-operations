package be.kdg.airfreight.loadplanning.acceptance.unit;

import be.kdg.airfreight.loadplanning.acceptance.common.Weights;
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
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadFinalized;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadPosition;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadingPolicy;
import be.kdg.airfreight.loadplanning.domain.flightcargo.Uld;
import be.kdg.airfreight.loadplanning.domain.flightcargo.UldCode;
import be.kdg.airfreight.loadplanning.domain.flightcargo.check.AllCargoLoadedCheck;
import be.kdg.airfreight.loadplanning.domain.flightcargo.check.LateralBalanceCheck;
import be.kdg.airfreight.loadplanning.domain.flightcargo.check.OffloadingCheck;
import be.kdg.airfreight.loadplanning.domain.loadplanner.LoadPlannerId;
import be.kdg.airfreight.loadplanning.infrastructure.floorplan.Boeing747400FFloorPlan;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

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
    /** The weight of a ULD when the scenario does not name it. */
    static final String ANY_WEIGHT = "1000 kg";
    private static final AirportCode ATH = new AirportCode("ATH");

    final FloorPlan floorPlan = Boeing747400FFloorPlan.build();
    FlightCargo cargo = FlightCargo.create(
        flightId("HJ221", LocalDate.of(2026, Month.JANUARY, 9), "BRU"),
        FlightInfo.scheduled(ATH, Boeing747400FFloorPlan.AIRCRAFT_TYPE,
            Instant.parse("2026-01-09T06:45:00Z"), Instant.parse("2026-01-09T10:00:00Z")), POLICY);
    final LoadPlannerId planner = LoadPlannerId.create();
    /** The moment the scenario takes place; the loading period of the flight is open at NOW. */
    Instant now = NOW;
    /** The result of one check, which the Then steps of that check read. */
    LoadCheckResult checkResult;
    /** The results of all checks. */
    LoadCheckReport report;
    /** What the last When step was refused with, or null when it was accepted. */
    Throwable error;
    /** A fragment of the reason the scenario expects a refusal for, set by the step that causes it. */
    String expectedRefusal;
    /** Set by the When steps: a step that is both a Given and a Then sets up before it and checks after it. */
    boolean acted;
    private int uldCount;
    private final List<FlightCargo> otherFlights = new ArrayList<>();

    void assertFlight(String flightNumber) {
        assertThat(cargo.getFlightId().flightNumber().value()).as("flight").isEqualTo(flightNumber);
    }

    void assertFlight(String flightNumber, LocalDate date, String departureAirport) {
        assertThat(cargo.getFlightId()).as("flight").isEqualTo(flightId(flightNumber, date, departureAirport));
    }

    /** Another flight next to cargo, with the same times on its own date ("HJ221 of 16 January"). */
    void alsoExists(String flightNumber, LocalDate date, String departureAirport) {
        Duration later = Duration.ofDays(ChronoUnit.DAYS.between(cargo.getFlightId().scheduledDepartureDate(), date));
        FlightInfo info = cargo.getFlightInfo();
        otherFlights.add(FlightCargo.create(flightId(flightNumber, date, departureAirport),
            FlightInfo.scheduled(info.arrivalAirport(), info.aircraftType(), info.std().plus(later),
                info.sta().plus(later)), POLICY));
    }

    /** cargo and the flights the scenario added with alsoExists. */
    List<FlightCargo> allFlights() {
        return Stream.concat(Stream.of(cargo), otherFlights.stream()).toList();
    }

    /** Books a ULD for ATH on one of allFlights. */
    void bookOn(String flightNumber, LocalDate date, UldCode code) {
        bookOn(flightNumber, date, code, ANY_WEIGHT);
    }

    /** The weight is read here, so a weight that Weight refuses is refused by this call. */
    void bookOn(String flightNumber, LocalDate date, UldCode code, String weight) {
        FlightCargo flight = allFlights().stream()
            .filter(candidate -> candidate.getFlightId().flightNumber().value().equals(flightNumber)
                && candidate.getFlightId().scheduledDepartureDate().equals(date))
            .findFirst()
            .orElseThrow(() -> new AssertionError("The scenario has no flight " + flightNumber + " of " + date));
        flight.book(code, Weights.of(weight), ATH, now);
    }

    void assertWithinLoadingPeriod() {
        assertThat(cargo.loadingPeriod(POLICY).contains(now)).as("%s is within the loading period", now).isTrue();
    }

    /** The flight as it is, only with another destination. */
    void goesTo(String airport) {
        FlightInfo info = cargo.getFlightInfo();
        cargo.updateFlightInfo(new FlightInfo(new AirportCode(airport), info.aircraftType(), info.std(), info.sta(),
            info.etd(), info.eta(), info.departed()));
    }

    /**
     * The flight with another STD and no delays. A new cargo, because the stored closing time of the loading
     * period does not follow a new STD: call it before anything is booked.
     */
    void departsAt(Instant std) {
        FlightInfo info = cargo.getFlightInfo();
        cargo = FlightCargo.create(cargo.getFlightId(),
            FlightInfo.scheduled(info.arrivalAirport(), info.aircraftType(), std, info.sta()), POLICY);
    }

    /** The ETD becomes the STD plus the departure delays, entered at now. */
    void delayDepartureBy(Duration delays) {
        cargo.changeEstimatedDeparture(cargo.getFlightInfo().std().plus(delays), now, POLICY);
    }

    /** Books a ULD for ATH. */
    void book(UldCode code, String weight) {
        book(code, weight, ATH.value());
    }

    void book(UldCode code, String weight, String destination) {
        cargo.book(code, Weights.of(weight), new AirportCode(destination), now);
    }

    void place(UldCode code, String position) {
        cargo.assign(code, floorPlan.positionNamed(position).orElseThrow(), planner, now, floorPlan, POLICY);
    }

    /** Books the ULD and puts it on the position. */
    void load(UldCode code, String weight, String destination, String position) {
        book(code, weight, destination);
        place(code, position);
    }

    /** Puts the weight on the position in a ULD for ATH. "0 kg" loads nothing: a ULD always weighs something. */
    void loadWeightOn(String position, String weight) {
        if (new BigDecimal(weight.split(" ")[0]).signum() == 0) {
            return;
        }
        load(nextCode(), weight, ATH.value(), position);
    }

    /** A code for a ULD the scenario does not name: AKE00001HJ, AKE00002HJ, ... */
    UldCode nextCode() {
        return new UldCode("AKE%05dHJ".formatted(++uldCount));
    }

    Uld uld(UldCode code) {
        return cargo.getUlds().stream()
            .filter(uld -> uld.getCode().equals(code))
            .findFirst()
            .orElseThrow();
    }

    LoadFinalized finalizeLoad(List<LoadCheck> checks) {
        return cargo.finalizeLoad(planner, now, checks, floorPlan, POLICY);
    }

    /** A position of the floor plan that holds no ULD. */
    String freePosition() {
        return floorPlan.routesByPosition().keySet().stream()
            .filter(position -> cargo.getUlds().stream().noneMatch(uld -> uld.isAt(position)))
            .map(LoadPosition::name)
            .findFirst()
            .orElseThrow();
    }

    void check(LoadCheck check) {
        checkResult = cargo.checkLoad(List.of(check), floorPlan, now, POLICY).results().getFirst();
    }

    void checkAll() {
        report = cargo.checkLoad(CHECKS, floorPlan, now, POLICY);
    }

    /** For the When steps: runs the action and keeps its refusal in error. */
    void attempt(ThrowingCallable action) {
        acted = true;
        error = catchThrowable(action);
    }

    void assertAccepted() {
        assertThat(error).as("refusal").isNull();
    }

    void assertRefusedForTheExpectedReason() {
        assertThat(expectedRefusal).as("expected refusal, set by the step that causes it").isNotNull();
        assertThat(error)
            .isInstanceOf(DomainRuleViolationException.class)
            .hasMessageContaining(expectedRefusal);
    }

    private static FlightId flightId(String flightNumber, LocalDate date, String departureAirport) {
        return new FlightId(new FlightNumber(flightNumber), date, new AirportCode(departureAirport));
    }
}
