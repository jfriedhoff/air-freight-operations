package be.kdg.airfreight.loadplanning.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import be.kdg.airfreight.loadplanning.application.command.AssignLoadPositionCommand;
import be.kdg.airfreight.loadplanning.application.command.ClearLoadPositionCommand;
import be.kdg.airfreight.loadplanning.application.command.FinalizeLoadCommand;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargo;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargoId;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargoRepository;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FloorPlan;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FloorPlanCatalog;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadCheck;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadCheckReport;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadFinalized;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadPosition;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadPositionCleared;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadingPolicy;
import be.kdg.airfreight.loadplanning.domain.loadplanner.LoadPlannerId;
import be.kdg.airfreight.loadplanning.domain.loadplanner.LoadPlannerRepository;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** What a load planner does to the cargo of a flight. Every rule is in FlightCargo. */
@Service
@Transactional
public class LoadService {
    private final FlightCargoRepository cargos;
    private final LoadPlannerRepository loadPlanners;
    private final FloorPlanCatalog floorPlans;
    private final List<LoadCheck> checks;
    private final LoadingPolicy policy;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public LoadService(FlightCargoRepository cargos, LoadPlannerRepository loadPlanners,
                       FloorPlanCatalog floorPlans, List<LoadCheck> checks,
                       LoadingPolicy policy, ApplicationEventPublisher events, Clock clock) {
        this.cargos = cargos;
        this.loadPlanners = loadPlanners;
        this.floorPlans = floorPlans;
        this.checks = checks;
        this.policy = policy;
        this.events = events;
        this.clock = clock;
    }

    /** The cargo of a flight as it stands at this moment. */
    @Transactional(readOnly = true)
    public FlightCargoSnapshot get(FlightCargoId id) {
        FlightCargo cargo = find(id);
        return snapshot(cargo, floorPlanOf(cargo), clock.instant());
    }

    /**
     * The cargos of the flights of one day, without the departed flights unless they are asked for.
     * Without a day it is today, and the answer names the day that was used.
     * Provisional (question 9 of the analysis): today is the scheduled departure date in UTC.
     */
    @Transactional(readOnly = true)
    public FlightsToLoad flightsToLoad(@Nullable LocalDate scheduledDepartureDate, boolean includeDeparted) {
        LocalDate day = scheduledDepartureDate != null
            ? scheduledDepartureDate
            : LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
        List<FlightCargo> flights = cargos.findByScheduledDepartureDate(day).stream()
            .filter(cargo -> includeDeparted || cargo.isToBeLoaded())
            .toList();
        return new FlightsToLoad(day, flights);
    }

    public FlightCargo assign(AssignLoadPositionCommand command) {
        FlightCargo cargo = find(command.flightCargoId());
        FloorPlan floorPlan = floorPlanOf(cargo);
        LoadPosition position = floorPlan.position(command.positionName());
        events.publishEvent(cargo.assign(command.uldCode(), position, command.by(), clock.instant(), floorPlan,
            policy));
        cargos.save(cargo);
        return cargo;
    }

    public FlightCargo clear(ClearLoadPositionCommand command) {
        requireRegistered(command.by());
        FlightCargo cargo = find(command.flightCargoId());
        LoadPositionCleared cleared = cargo.clear(command.uldCode(), command.by(), clock.instant(), policy);
        cargos.save(cargo);
        events.publishEvent(cleared);
        return cargo;
    }

    @Transactional(readOnly = true)
    public LoadCheckReport checkLoad(FlightCargoId id) {
        FlightCargo cargo = find(id);
        return cargo.checkLoad(checks, floorPlanOf(cargo));
    }

    /**
     * Finalizes the load when every hard check passes. Either way the report comes back, so the screen
     * shows the same report on success and on failure. The aggregate runs the checks again itself.
     */
    public LoadCheckReport finalizeLoad(FinalizeLoadCommand command) {
        requireRegistered(command.by());
        FlightCargo cargo = find(command.flightCargoId());
        FloorPlan floorPlan = floorPlanOf(cargo);
        Instant now = clock.instant();
        LoadCheckReport report = cargo.checkLoad(checks, floorPlan);
        if (report.hardChecksPassed()) {
            LoadFinalized finalized = cargo.finalizeLoad(command.by(), now, checks, floorPlan, policy);
            cargos.save(cargo);
            events.publishEvent(finalized);
        }
        return report;
    }

    /** A change is recorded with a planner that exists. Reading a second aggregate in the transaction is fine. */
    private void requireRegistered(LoadPlannerId planner) {
        if (loadPlanners.findById(planner).isEmpty()) {
            throw planner.notRegistered();
        }
    }

    private FlightCargoSnapshot snapshot(FlightCargo cargo, FloorPlan floorPlan, Instant now) {
        return new FlightCargoSnapshot(cargo, floorPlan, cargo.loadingPeriod(policy), cargo.loadCanChange(now, policy));
    }

    private FlightCargo find(FlightCargoId id) {
        return cargos.findById(id).orElseThrow(id::notFound);
    }

    private FloorPlan floorPlanOf(FlightCargo cargo) {
        return floorPlans.floorPlanFor(cargo.getFlightInfo().aircraftType());
    }
}
