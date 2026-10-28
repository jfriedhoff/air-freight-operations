package be.kdg.airfreight.loadplanning.infrastructure.messaging;

import be.kdg.airfreight.loadplanning.domain.flightcargo.AirportCode;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargoId;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightId;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightNumber;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadFinalized;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadPosition;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadedUld;
import be.kdg.airfreight.loadplanning.domain.flightcargo.Side;
import be.kdg.airfreight.loadplanning.domain.flightcargo.UldCode;
import be.kdg.airfreight.loadplanning.domain.flightcargo.Weight;
import be.kdg.airfreight.loadplanning.domain.flightcargo.WeightUnit;
import be.kdg.airfreight.loadplanning.domain.loadplanner.LoadPlannerId;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static be.kdg.airfreight.loadplanning.Contracts.example;
import static be.kdg.airfreight.loadplanning.Contracts.fieldsOf;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Load Planning against the example message in contracts/. The finalized load it publishes must have exactly the
 * fields of the example, because Messaging tests its reader with the same example.
 */
class ContractTest {
    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final String LOAD_FINALIZED = "load-planning.flight-cargo.load-finalized";

    @Test
    void aFinalizedLoadIsPublishedWithTheFieldsOfItsExample() {
        LoadFinalized event = new LoadFinalized(FlightCargoId.create(),
            new FlightId(new FlightNumber("HJ221"), LocalDate.of(2026, 1, 9), new AirportCode("BRU")),
            List.of(new LoadedUld(new UldCode("AKE16689R7"), new LoadPosition("CL", Side.LEFT),
                new Weight(new BigDecimal("1250"), WeightUnit.KG), new AirportCode("ATH"))),
            LoadPlannerId.create(), Instant.parse("2026-01-09T06:00:00Z"));

        LoadFinalizedJson published = LoadFinalizedJson.from(event, UUID.randomUUID());

        assertThat(published.type()).isEqualTo(LOAD_FINALIZED);
        assertThat(fieldsOf(MAPPER.writeValueAsString(published))).isEqualTo(fieldsOf(example(LOAD_FINALIZED)));
    }
}
