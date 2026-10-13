package be.kdg.airfreight.messaging.domain;

/** The IATA airport list. */
public interface AirportCatalog {
    boolean contains(AirportCode code);

    /** The airport with this code. A code that the list does not have is refused. */
    default AirportCode airport(String code) {
        AirportCode airport = new AirportCode(code);
        if (!contains(airport)) {
            throw new DomainRuleViolationException("Airport '" + code + "' is not in the IATA list");
        }
        return airport;
    }
}
