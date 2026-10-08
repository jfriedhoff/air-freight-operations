package be.kdg.airfreight.flightoperations.domain.flightplanner;

/** Someone acts as a flight planner but is not registered as one in this context. */
public class UnknownFlightPlannerException extends RuntimeException {
    public UnknownFlightPlannerException(String message) {
        super(message);
    }
}
