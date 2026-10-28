package be.kdg.airfreight.messaging.infrastructure.messaging;

import be.kdg.airfreight.messaging.domain.AircraftRegistration;
import be.kdg.airfreight.messaging.domain.AirportCode;
import be.kdg.airfreight.messaging.domain.FlightId;
import be.kdg.airfreight.messaging.domain.FlightNumber;
import be.kdg.airfreight.messaging.domain.flight.ArrivalEstimateChanged;
import be.kdg.airfreight.messaging.domain.flight.DepartureEstimateChanged;
import be.kdg.airfreight.messaging.domain.flight.FlightDeparted;
import be.kdg.airfreight.messaging.domain.flight.FlightLanded;
import be.kdg.airfreight.messaging.domain.load.LoadFinalized;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static be.kdg.airfreight.messaging.Contracts.example;
import static be.kdg.airfreight.messaging.Contracts.fieldsOf;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Messaging against the example messages in contracts/. Its readers must understand the events of Flight
 * Operations and Load Planning, because those services check their JSON against the same examples. The message
 * that tells Flight Operations the CPM is written must have exactly the fields of its example, because Flight
 * Operations reads that example.
 */
class ContractTest {
    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final FlightId HJ221 =
        new FlightId(new FlightNumber("HJ221"), LocalDate.of(2026, 1, 9), new AirportCode("BRU"));
    private static final AirportCode ATH = new AirportCode("ATH");
    private static final AircraftRegistration SXBAA = new AircraftRegistration("SXBAA");

    private final JsonFlightEventReader flightEvents = new JsonFlightEventReader(MAPPER);
    private final JsonLoadFinalizedReader loads = new JsonLoadFinalizedReader(MAPPER);

    @Test
    void aNewEtdAndEtaCanBeRead() {
        assertThat(flightEvents.read(example("flight-operations.flight.departure-estimate-changed")))
            .isInstanceOfSatisfying(DepartureEstimateChanged.class, event -> {
                assertThat(event.flightId()).isEqualTo(HJ221);
                assertThat(event.registration()).isEqualTo(SXBAA);
                assertThat(event.etd()).isEqualTo(Instant.parse("2026-01-09T06:45:00Z"));
            });
        assertThat(flightEvents.read(example("flight-operations.flight.arrival-estimate-changed")))
            .isInstanceOfSatisfying(ArrivalEstimateChanged.class, event -> {
                assertThat(event.flightId()).isEqualTo(HJ221);
                assertThat(event.arrivalAirport()).isEqualTo(ATH);
                assertThat(event.registration()).isEqualTo(SXBAA);
                assertThat(event.eta()).isEqualTo(Instant.parse("2026-01-09T10:00:00Z"));
            });
    }

    @Test
    void aDepartureAndAnArrivalCanBeRead() {
        assertThat(flightEvents.read(example("flight-operations.flight.departed")))
            .isInstanceOfSatisfying(FlightDeparted.class, event -> {
                assertThat(event.flightId()).isEqualTo(HJ221);
                assertThat(event.registration()).isEqualTo(SXBAA);
                assertThat(event.atd()).isEqualTo(Instant.parse("2026-01-09T06:52:00Z"));
            });
        assertThat(flightEvents.read(example("flight-operations.flight.landed")))
            .isInstanceOfSatisfying(FlightLanded.class, event -> {
                assertThat(event.flightId()).isEqualTo(HJ221);
                assertThat(event.arrivalAirport()).isEqualTo(ATH);
                assertThat(event.registration()).isEqualTo(SXBAA);
                assertThat(event.ata()).isEqualTo(Instant.parse("2026-01-09T10:04:00Z"));
            });
    }

    @Test
    void aFinalizedLoadCanBeRead() {
        LoadFinalized load = loads.read(example("load-planning.flight-cargo.load-finalized"));

        assertThat(load.flightId()).isEqualTo(HJ221);
        assertThat(load.load()).singleElement().satisfies(line -> {
            assertThat(line.uldCode()).isEqualTo("AKE16689R7");
            assertThat(line.position()).isEqualTo("CL");
            assertThat(line.weight()).isEqualByComparingTo("1250");
            assertThat(line.weightUnit()).isEqualTo("KG");
            assertThat(line.destination()).isEqualTo(ATH);
        });
    }

    /** OutboxWriter publishes this record once LoadService has written the CPM. */
    @Test
    void theMessageThatTheCpmIsWrittenHasTheFieldsOfItsExample() {
        CpmCreatedJson published = new CpmCreatedJson(UUID.randomUUID(), MessageTypes.CPM_CREATED,
            MessageTypes.VERSION, Instant.parse("2026-01-09T06:00:01Z"), "HJ221", LocalDate.of(2026, 1, 9), "BRU");

        assertThat(fieldsOf(MAPPER.writeValueAsString(published))).isEqualTo(fieldsOf(example(MessageTypes.CPM_CREATED)));
    }
}
