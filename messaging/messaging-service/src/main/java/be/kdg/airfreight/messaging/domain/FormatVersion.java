package be.kdg.airfreight.messaging.domain;

import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
public record FormatVersion(int number) {
    public FormatVersion {
        if (number < 1) {
            throw new DomainRuleViolationException("A format version is 1 or higher");
        }
    }
}
