package be.kdg.airfreight.flightoperations.infrastructure.reference;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/** Reads a reference list from the classpath: one "code;text" per line. */
final class CsvResource {
    private CsvResource() {
    }

    static List<String[]> rows(String path) {
        InputStream resource = Objects.requireNonNull(CsvResource.class.getResourceAsStream(path),
            "Reference list " + path + " is missing");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8))) {
            return reader.lines()
                .filter(line -> !line.isBlank())
                .map(line -> line.split(";", 2))
                .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
