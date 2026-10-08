package be.kdg.airfreight.flightoperations.infrastructure.reference;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import be.kdg.airfreight.flightoperations.domain.flight.AirportCatalog;
import be.kdg.airfreight.flightoperations.domain.flight.AirportCode;
import org.springframework.stereotype.Component;

/** The IATA airports, read once from iata/airports.csv. */
@Component
public class ResourceAirportCatalog implements AirportCatalog {
    private final Map<AirportCode, String> names = CsvResource.rows("/iata/airports.csv").stream()
        .collect(Collectors.toMap(row -> new AirportCode(row[0]), row -> row[1]));

    @Override
    public Optional<String> nameOf(AirportCode code) {
        return Optional.ofNullable(names.get(code));
    }
}
