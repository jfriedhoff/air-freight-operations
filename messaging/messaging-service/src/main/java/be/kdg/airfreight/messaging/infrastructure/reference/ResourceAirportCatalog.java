package be.kdg.airfreight.messaging.infrastructure.reference;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import be.kdg.airfreight.messaging.domain.AirportCatalog;
import be.kdg.airfreight.messaging.domain.AirportCode;
import org.springframework.stereotype.Component;

/** The IATA airports, read once from iata/airports.csv: one "code;name" per line, the same list as Flight Operations. */
@Component
class ResourceAirportCatalog implements AirportCatalog {
    private static final String LIST = "/iata/airports.csv";

    private final Set<AirportCode> airports = read();

    @Override
    public boolean contains(AirportCode code) {
        return airports.contains(code);
    }

    private static Set<AirportCode> read() {
        InputStream resource = Objects.requireNonNull(ResourceAirportCatalog.class.getResourceAsStream(LIST),
            "Reference list " + LIST + " is missing");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8))) {
            return reader.lines()
                .filter(line -> !line.isBlank())
                .map(line -> new AirportCode(line.split(";", 2)[0]))
                .collect(Collectors.toUnmodifiableSet());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
