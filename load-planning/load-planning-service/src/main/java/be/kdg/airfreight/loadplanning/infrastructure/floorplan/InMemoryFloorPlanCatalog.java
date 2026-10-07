package be.kdg.airfreight.loadplanning.infrastructure.floorplan;

import java.util.Map;

import be.kdg.airfreight.loadplanning.domain.DomainRuleViolationException;
import be.kdg.airfreight.loadplanning.domain.flightcargo.AircraftType;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FloorPlan;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FloorPlanCatalog;
import org.springframework.stereotype.Component;

@Component
class InMemoryFloorPlanCatalog implements FloorPlanCatalog {
    private final Map<AircraftType, FloorPlan> floorPlans =
        Map.of(Boeing747400FFloorPlan.AIRCRAFT_TYPE, Boeing747400FFloorPlan.build());

    @Override
    public FloorPlan floorPlanFor(AircraftType aircraftType) {
        FloorPlan floorPlan = floorPlans.get(aircraftType);
        if (floorPlan == null) {
            throw new DomainRuleViolationException("No floor plan is known for aircraft type " + aircraftType.code());
        }
        return floorPlan;
    }
}
