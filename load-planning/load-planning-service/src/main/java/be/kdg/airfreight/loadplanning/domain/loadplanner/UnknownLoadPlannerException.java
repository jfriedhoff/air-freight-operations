package be.kdg.airfreight.loadplanning.domain.loadplanner;

/** Someone acts as a load planner but is not registered as one in this context. */
public class UnknownLoadPlannerException extends RuntimeException {
    public UnknownLoadPlannerException(String message) {
        super(message);
    }
}
