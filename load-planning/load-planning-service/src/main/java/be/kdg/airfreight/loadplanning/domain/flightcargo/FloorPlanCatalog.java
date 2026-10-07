package be.kdg.airfreight.loadplanning.domain.flightcargo;

public interface FloorPlanCatalog {
    FloorPlan floorPlanFor(AircraftType aircraftType);
}
