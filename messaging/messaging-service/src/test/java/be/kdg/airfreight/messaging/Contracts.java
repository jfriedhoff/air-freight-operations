package be.kdg.airfreight.messaging;

import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The example messages in contracts/ in the root of the repository: one per event that goes from one service to
 * another. build.gradle puts that folder on the test classpath. The service that publishes an event checks that
 * its JSON has the same fields as the example; the service that reads the event checks that its reader
 * understands the example. So a field that one side renames or drops fails a test on one of the two sides.
 */
public final class Contracts {
    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private Contracts() {
    }

    /** The example message of this type, as JSON text. */
    public static String example(String type) {
        try (InputStream in = Contracts.class.getResourceAsStream("/" + type + ".json")) {
            if (in == null) {
                throw new IllegalArgumentException("contracts/ has no example message for " + type);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** The names of the fields of a JSON message. A field of the elements of a list is named like "load[].uldCode". */
    public static Set<String> fieldsOf(String json) {
        Set<String> fields = new TreeSet<>();
        collect("", MAPPER.readValue(json, Object.class), fields);
        return fields;
    }

    private static void collect(String path, Object value, Set<String> fields) {
        if (value instanceof Map<?, ?> map) {
            map.forEach((name, child) -> {
                String childPath = path.isEmpty() ? name.toString() : path + "." + name;
                fields.add(childPath);
                collect(childPath, child, fields);
            });
        } else if (value instanceof List<?> list) {
            list.forEach(element -> collect(path + "[]", element, fields));
        }
    }
}
