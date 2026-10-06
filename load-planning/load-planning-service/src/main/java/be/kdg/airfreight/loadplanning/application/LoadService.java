package be.kdg.airfreight.loadplanning.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import be.kdg.airfreight.loadplanning.application.command.AssignLoadPositionCommand;
import be.kdg.airfreight.loadplanning.application.command.ClearLoadPositionCommand;
import be.kdg.airfreight.loadplanning.application.command.FinalizeLoadCommand;
import be.kdg.airfreight.loadplanning.domain.DomainRuleViolationException;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargo;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargoId;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargoRepository;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FloorPlan;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FloorPlanCatalog;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadCheck;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadCheckReport;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadPosition;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadingPolicy;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** What a load planner does to the cargo of a flight. Every rule is in FlightCargo. */
@Service
@Transactional
public class LoadService {
    private final FlightCargoRepository cargos;
    private final FloorPlanCatalog floorPlans;
    private final List<LoadCheck> checks;
    private final LoadingPolicy policy;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public LoadService(FlightCargoRepository cargos, FloorPlanCatalog floorPlans, List<LoadCheck> checks,
                       LoadingPolicy policy, ApplicationEventPublisher events, Clock clock) {
        this.cargos = cargos;
        this.floorPlans = floorPlans;
        this.checks = checks;
        this.policy = policy;
        this.events = events;
        this.clock = clock;
    }

    public FlightCargo assign(AssignLoadPositionCommand command) {
        FlightCargo cargo = find(command.flightCargoId());
        FloorPlan floorPlan = floorPlanOf(cargo);
        LoadPosition position = floorPlan.positionNamed(command.positionName())
            .orElseThrow(() -> new DomainRuleViolationException("Load position " + command.positionName()
                + " is not on the floor plan of aircraft type " + floorPlan.aircraftType().code()));
        events.publishEvent(cargo.assign(command.uldCode(), position, command.by(), clock.instant(), floorPlan,
            policy));
        cargos.save(cargo);
        return cargo;
    }

    public FlightCargo clear(ClearLoadPositionCommand command) {
        FlightCargo cargo = find(command.flightCargoId());
        events.publishEvent(cargo.clear(command.uldCode(), command.by(), clock.instant(), policy));
        cargos.save(cargo);
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
        FlightCargo cargo = find(command.flightCargoId());
        FloorPlan floorPlan = floorPlanOf(cargo);
        Instant now = clock.instant();
        LoadCheckReport report = cargo.checkLoad(checks, floorPlan);
        if (report.hardChecksPassed()) {
            events.publishEvent(cargo.finalizeLoad(command.by(), now, checks, floorPlan, policy));
            cargos.save(cargo);
        }
        return report;
    }

    private FlightCargo find(FlightCargoId id) {
        return cargos.findById(id).orElseThrow(id::notFound);
    }

    private FloorPlan floorPlanOf(FlightCargo cargo) {
        return floorPlans.floorPlanFor(cargo.getFlightInfo().aircraftType());
    }
}
