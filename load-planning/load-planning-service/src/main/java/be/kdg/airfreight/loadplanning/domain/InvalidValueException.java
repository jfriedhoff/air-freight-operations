package be.kdg.airfreight.loadplanning.domain;

/**
 * A value that is wrong in itself, whatever the state of the load: a malformed ULD code, a load position that
 * is not on the floor plan. A rule that depends on the state stays a DomainRuleViolationException.
 */
public class InvalidValueException extends DomainRuleViolationException {
    public InvalidValueException(String message) {
        super(message);
    }
}
