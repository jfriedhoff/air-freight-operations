package be.kdg.airfreight.loadplanning.domain.flightcargo;

/** A failed hard check prevents take-off; a failed soft check only warns. */
public enum CheckSeverity {
    HARD,
    SOFT
}
