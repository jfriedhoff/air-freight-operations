package be.kdg.airfreight.loadplanning.domain.flightcargo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

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
    private final List<Uld> ulds = new ArrayList<>();
    private boolean finalized;
    private @Nullable LoadPlannerId finalizedBy;
    private final @Nullable Long version;

    private FlightCargo(FlightCargoId id, FlightId flightId, FlightInfo flightInfo, @Nullable Long version) {
        this.id = Objects.requireNonNull(id, "Id cannot be null");
        this.flightId = Objects.requireNonNull(flightId, "Flight id cannot be null");
        this.flightInfo = Objects.requireNonNull(flightInfo, "Flight info cannot be null");
        this.version = version;
    }

    public static FlightCargo create(FlightId flightId, FlightInfo flightInfo) {
        return new FlightCargo(FlightCargoId.create(), flightId, flightInfo, null);
    }

    public static FlightCargo hydrate(FlightCargoId id, FlightId flightId, FlightInfo flightInfo, List<Uld> ulds,
                                      boolean finalized, @Nullable LoadPlannerId finalizedBy,
                                      @Nullable Long version) {
        FlightCargo cargo = new FlightCargo(id, flightId, flightInfo, version);
        cargo.ulds.addAll(ulds);
        cargo.finalized = finalized;
        cargo.finalizedBy = finalizedBy;
        return cargo;
    }

    /** Replaces the copy of the flight, unless it is older than or equal to the one we have. */
    public void updateFlightInfo(FlightInfo newer) {
        Objects.requireNonNull(newer, "Flight info cannot be null");
        if (newer.version() > flightInfo.version()) {
            flightInfo = newer;
        }
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
            throw new DomainRuleViolationException("Load position " + position.name()
                + " is not on the floor plan of aircraft type " + floorPlan.aircraftType().code());
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

    public LoadCheckReport checkLoad(List<LoadCheck> checks, FloorPlan floorPlan) {
        return new LoadCheckReport(checks.stream()
            .map(check -> check.check(this, floorPlan))
            .toList());
    }

    /** Runs the checks again: the load may have changed since the last report. */
    public LoadFinalized finalizeLoad(LoadPlannerId by, Instant now, List<LoadCheck> checks, FloorPlan floorPlan,
                                      LoadingPolicy policy) {
        requireLoadCanChange(now, policy);
        if (!checkLoad(checks, floorPlan).hardChecksPassed()) {
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

    /** From STD minus the opening time (included) until the current ETD minus the closing time (not included). */
    public LoadingPeriod loadingPeriod(LoadingPolicy policy) {
        return new LoadingPeriod(
            flightInfo.std().minus(policy.opensBeforeScheduledDeparture()),
            flightInfo.etd().minus(policy.closesBeforeEstimatedDeparture()));
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
        if (finalized) {
            throw new DomainRuleViolationException("The load is finalized and can no longer change");
        }
        if (flightInfo.departed()) {
            throw new DomainRuleViolationException("The load can no longer change: the flight has departed");
        }
        if (!loadingPeriod(policy).contains(now)) {
            throw new DomainRuleViolationException("The load can only change within the loading period");
        }
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
