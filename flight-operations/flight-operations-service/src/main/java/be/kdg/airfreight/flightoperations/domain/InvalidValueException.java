package be.kdg.airfreight.flightoperations.domain;

/**
 * A value that is wrong in itself, whatever the state of the flight: a delay of 0 minutes, an unknown
 * delay reason. A rule that depends on the state stays a DomainRuleViolationException.
 */
public class InvalidValueException extends DomainRuleViolationException {
    public InvalidValueException(String message) {
        super(message);
    }
}
