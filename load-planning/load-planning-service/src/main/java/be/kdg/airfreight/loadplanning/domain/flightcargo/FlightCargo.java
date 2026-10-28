package be.kdg.airfreight.loadplanning.domain.flightcargo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import be.kdg.airfreight.loadplanning.domain.DomainRuleViolationException;
import be.kdg.airfreight.loadplanning.domain.loadplanner.LoadPlannerId;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.jspecify.annotations.Nullable;

@AggregateRoot
public class FlightCargo {
    @Identity
    private final FlightCargoId id;
    private final FlightId flightId;
    private FlightInfo flightInfo;
    private Instant loadingClosesAt;
    private final List<Uld> ulds = new ArrayList<>();
    private boolean finalized;
    private @Nullable LoadPlannerId finalizedBy;
    private final @Nullable Long version;

    private FlightCargo(FlightCargoId id, FlightId flightId, FlightInfo flightInfo, Instant loadingClosesAt,
                        @Nullable Long version) {
        this.id = Objects.requireNonNull(id, "Id cannot be null");
        this.flightId = Objects.requireNonNull(flightId, "Flight id cannot be null");
        this.flightInfo = Objects.requireNonNull(flightInfo, "Flight info cannot be null");
        this.loadingClosesAt = Objects.requireNonNull(loadingClosesAt, "Closing time cannot be null");
        this.version = version;
    }

    public static FlightCargo create(FlightId flightId, FlightInfo flightInfo, LoadingPolicy policy) {
        return new FlightCargo(FlightCargoId.create(), flightId, flightInfo,
            flightInfo.etd().minus(policy.closesBeforeEstimatedDeparture()), null);
    }

    public static FlightCargo hydrate(FlightCargoId id, FlightId flightId, FlightInfo flightInfo,
                                      Instant loadingClosesAt, List<Uld> ulds, boolean finalized,
                                      @Nullable LoadPlannerId finalizedBy, @Nullable Long version) {
        FlightCargo cargo = new FlightCargo(id, flightId, flightInfo, loadingClosesAt, version);
        cargo.ulds.addAll(ulds);
        cargo.finalized = finalized;
        cargo.finalizedBy = finalizedBy;
        return cargo;
    }

    /** Replaces the copy of the flight. A new ETD goes through changeEstimatedDeparture. */
    public void updateFlightInfo(FlightInfo newer) {
        flightInfo = Objects.requireNonNull(newer, "Flight info cannot be null");
    }

    /**
     * A new ETD moves the end of the loading period, but only while the period has not ended:
     * once closed, always closed (analysis question 8). Removing a delay can close it.
     */
    public void changeEstimatedDeparture(Instant etd, Instant now, LoadingPolicy policy) {
        if (now.isBefore(loadingClosesAt)) {
            loadingClosesAt = etd.minus(policy.closesBeforeEstimatedDeparture());
        }
        flightInfo = flightInfo.withEstimatedDeparture(etd);
    }

    public boolean hasBooked(UldCode code) {
        return ulds.stream().anyMatch(uld -> uld.getCode().equals(code));
    }

    public UldBooked book(UldCode code, Weight weight, AirportCode destination, Instant now) {
        if (flightInfo.departed()) {
            throw new DomainRuleViolationException("Cargo can no longer be booked: the flight has departed");
        }
        if (ulds.stream().anyMatch(uld -> uld.getCode().equals(code))) {
            throw new DomainRuleViolationException("ULD " + code.value() + " is already booked on this flight");
        }
        ulds.add(new Uld(code, weight, destination));
        return new UldBooked(id, flightId, code, weight, destination, now);
    }

    public LoadPositionAssigned assign(UldCode code, LoadPosition position, LoadPlannerId by, Instant now,
                                       FloorPlan floorPlan, LoadingPolicy policy) {
        requireLoadCanChange(now, policy);
        Uld uld = uldWith(code);
        if (!floorPlan.contains(position)) {
            throw floorPlan.notOnTheFloorPlan(position.name());
        }
        ulds.stream()
            .filter(other -> other.isAt(position) && !other.equals(uld))
            .findFirst()
            .ifPresent(other -> {
                throw new DomainRuleViolationException("Load position " + position.name()
                    + " already holds ULD " + other.getCode().value());
            });
        uld.assign(position, by);
        return new LoadPositionAssigned(id, flightId, code, position, by, now);
    }

    public LoadPositionCleared clear(UldCode code, LoadPlannerId by, Instant now, LoadingPolicy policy) {
        requireLoadCanChange(now, policy);
        Uld uld = uldWith(code);
        LoadPosition position = uld.getPosition();
        if (position == null) {
            throw new DomainRuleViolationException("ULD " + code.value() + " has no load position");
        }
        uld.clear(by);
        return new LoadPositionCleared(id, flightId, code, position, by, now);
    }

    /**
     * Check Load: every check on the load as it stands. It changes nothing. A load planner may only ask it within
     * the loading period (case, "Load checks").
     */
    public LoadCheckReport checkLoad(List<LoadCheck> checks, FloorPlan floorPlan, Instant now, LoadingPolicy policy) {
        if (!loadingPeriod(policy).contains(now)) {
            throw new DomainRuleViolationException("The load can only be checked within the loading period");
        }
        return runChecks(checks, floorPlan);
    }

    /** Runs the checks again: the load may have changed since the last report. */
    public LoadFinalized finalizeLoad(LoadPlannerId by, Instant now, List<LoadCheck> checks, FloorPlan floorPlan,
                                      LoadingPolicy policy) {
        requireLoadCanChange(now, policy);
        if (!runChecks(checks, floorPlan).hardChecksPassed()) {
            throw new DomainRuleViolationException("The load cannot be finalized: a hard load check fails");
        }
        finalized = true;
        finalizedBy = Objects.requireNonNull(by, "Load planner cannot be null");
        List<LoadedUld> load = getLoad().stream()
            .map(uld -> new LoadedUld(uld.getCode(), Objects.requireNonNull(uld.getPosition()), uld.getWeight(),
                uld.getDestination()))
            .toList();
        return new LoadFinalized(id, flightId, load, by, now);
    }

    /** Whether a load planner can still change the load at this moment. A page uses it to offer the actions. */
    public boolean loadCanChange(Instant now, LoadingPolicy policy) {
        return whyTheLoadCannotChange(now, policy).isEmpty();
    }

    /** A flight still has to be loaded until it has departed. */
    public boolean isToBeLoaded() {
        return !flightInfo.departed();
    }

    /** From STD minus the opening time (included) until the stored closing time (not included). */
    public LoadingPeriod loadingPeriod(LoadingPolicy policy) {
        return new LoadingPeriod(flightInfo.std().minus(policy.opensBeforeScheduledDeparture()), loadingClosesAt);
    }

    public FlightCargoId getId() {
        return id;
    }

    public FlightId getFlightId() {
        return flightId;
    }

    public FlightInfo getFlightInfo() {
        return flightInfo;
    }

    public Instant getLoadingClosesAt() {
        return loadingClosesAt;
    }

    public List<Uld> getUlds() {
        return Collections.unmodifiableList(ulds);
    }

    /** The load: the ULDs that have a load position. */
    public List<Uld> getLoad() {
        return ulds.stream().filter(Uld::hasPosition).toList();
    }

    public boolean isFinalized() {
        return finalized;
    }

    public @Nullable LoadPlannerId getFinalizedBy() {
        return finalizedBy;
    }

    public @Nullable Long getVersion() {
        return version;
    }

    private void requireLoadCanChange(Instant now, LoadingPolicy policy) {
        whyTheLoadCannotChange(now, policy).ifPresent(reason -> {
            throw new DomainRuleViolationException(reason);
        });
    }

    /** The one place that says when the load is frozen: the reason, or nothing when it can change. */
    private Optional<String> whyTheLoadCannotChange(Instant now, LoadingPolicy policy) {
        if (finalized) {
            return Optional.of("The load is finalized and can no longer change");
        }
        if (flightInfo.departed()) {
            return Optional.of("The load can no longer change: the flight has departed");
        }
        if (!loadingPeriod(policy).contains(now)) {
            return Optional.of("The load can only change within the loading period");
        }
        return Optional.empty();
    }

    private LoadCheckReport runChecks(List<LoadCheck> checks, FloorPlan floorPlan) {
        return new LoadCheckReport(checks.stream()
            .map(check -> check.check(this, floorPlan))
            .toList());
    }

    private Uld uldWith(UldCode code) {
        return ulds.stream()
            .filter(uld -> uld.getCode().equals(code))
            .findFirst()
            .orElseThrow(code::notFound);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        return this == other || other instanceof FlightCargo cargo && id.equals(cargo.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
