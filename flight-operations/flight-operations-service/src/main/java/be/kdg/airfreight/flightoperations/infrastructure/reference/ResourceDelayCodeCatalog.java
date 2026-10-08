package be.kdg.airfreight.flightoperations.infrastructure.reference;

import java.util.List;

import be.kdg.airfreight.flightoperations.domain.flight.DelayCode;
import be.kdg.airfreight.flightoperations.domain.flight.DelayCodeCatalog;
import be.kdg.airfreight.flightoperations.domain.flight.DelayReason;
import org.springframework.stereotype.Component;

/** The IATA delay codes, read once from iata/delay-codes.csv. */
@Component
public class ResourceDelayCodeCatalog implements DelayCodeCatalog {
    private final List<DelayReason> reasons = CsvResource.rows("/iata/delay-codes.csv").stream()
        .map(row -> new DelayReason(new DelayCode(row[0]), row[1]))
        .toList();

    @Override
    public List<DelayReason> all() {
        return reasons;
    }
}
