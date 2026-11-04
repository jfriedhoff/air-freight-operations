package be.kdg.airfreight.messaging.infrastructure.messaging;

import be.kdg.airfreight.messaging.domain.InvalidMessageException;
import be.kdg.airfreight.messaging.domain.MessageId;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonLoadFinalizedReaderTest {
    private static final String MESSAGE_ID = "0b6f2a7e-3c41-4d8e-9a52-1f0c6d7e8a05";

    private final JsonLoadFinalizedReader reader = new JsonLoadFinalizedReader(JsonMapper.builder().build());

    @Test
    void aLoadWithAnEmptyElementIsAnInvalidMessageUnderItsOwnId() {
        assertThatThrownBy(() -> reader.read(loadFinalized("[null]")))
            .isInstanceOf(InvalidMessageException.class)
            .hasMessageContaining("load[]")
            .satisfies(e -> assertThat(((InvalidMessageException) e).messageId())
                .contains(new MessageId(UUID.fromString(MESSAGE_ID))));
    }

    @Test
    void aLoadWithAnEmptyElementBetweenTwoLinesIsInvalidToo() {
        String line = """
            {"uldCode": "AKE16689R7", "position": "CL", "weight": 1250, "weightUnit": "KG", "destination": "ATH"}""";
        assertThatThrownBy(() -> reader.read(loadFinalized("[" + line + ", null, " + line + "]")))
            .isInstanceOf(InvalidMessageException.class)
            .hasMessageContaining("load[]");
    }

    @Test
    void anEmptyLoadIsValid() {
        assertThat(reader.read(loadFinalized("[]")).load()).isEmpty();
    }

    private static String loadFinalized(String load) {
        return """
            {"messageId": "%s", "type": "load-planning.flight-cargo.load-finalized", "version": 1,
             "occurredOn": "2026-01-09T06:00:00Z", "flightNumber": "HJ221", "scheduledDepartureDate": "2026-01-09",
             "departureAirport": "BRU", "load": %s}""".formatted(MESSAGE_ID, load);
    }
}
