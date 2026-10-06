package be.kdg.airfreight.loadplanning.domain.flightcargo;

/** A rule about the load as a whole. One implementation per rule. */
public interface LoadCheck {
    String name();

    CheckSeverity severity();

    LoadCheckResult check(FlightCargo cargo, FloorPlan floorPlan);
}
