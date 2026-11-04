package be.kdg.airfreight.messaging.infrastructure.messaging;

import be.kdg.airfreight.messaging.domain.AircraftRegistration;
import be.kdg.airfreight.messaging.domain.AirportCode;
import be.kdg.airfreight.messaging.domain.FlightId;
import be.kdg.airfreight.messaging.domain.FlightNumber;
import be.kdg.airfreight.messaging.domain.flight.ArrivalEstimateChanged;
import be.kdg.airfreight.messaging.domain.flight.DepartureEstimateChanged;
import be.kdg.airfreight.messaging.domain.flight.FlightAnnounced;
import be.kdg.airfreight.messaging.domain.flight.FlightDeparted;
import be.kdg.airfreight.messaging.domain.flight.FlightEvent;
import be.kdg.airfreight.messaging.domain.flight.FlightLanded;
import be.kdg.airfreight.messaging.domain.flight.FlightRemoved;
import be.kdg.airfreight.messaging.domain.load.LoadFinalized;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static be.kdg.airfreight.messaging.Contracts.example;
import static be.kdg.airfreight.messaging.Contracts.fieldsOf;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Messaging against the example messages in contracts/. Its readers must understand the events of Flight
 * Operations and Load Planning, because those services check their JSON against the same examples. The message
 * that tells Flight Operations the CPM is written, the scheduled flights, the ULD bookings and the flight events it
 * passes on must have exactly the fields of their examples, because Flight Operations and Load Planning read those
 * examples.
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
    void anAnnouncedAndARemovedFlightCanBeRead() {
        assertThat(flightEvents.read(example("flight-operations.flight.announced")))
            .isInstanceOfSatisfying(FlightAnnounced.class, event -> {
                assertThat(event.flightId()).isEqualTo(HJ221);
                assertThat(event.arrivalAirport()).isEqualTo(ATH);
                assertThat(event.registration()).isEqualTo(SXBAA);
                assertThat(event.std()).isEqualTo(Instant.parse("2026-01-09T06:30:00Z"));
                assertThat(event.sta()).isEqualTo(Instant.parse("2026-01-09T09:45:00Z"));
            });
        assertThat(flightEvents.read(example("flight-operations.flight.removed")))
            .isInstanceOfSatisfying(FlightRemoved.class, event -> assertThat(event.flightId()).isEqualTo(HJ221));
    }

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

    /**
     * FlightEventService reads the event of Flight Operations and OutboxWriter passes it on under its own type,
     * with the same occurredOn: Load Planning judges the loading period by it.
     */
    @ParameterizedTest
    @ValueSource(strings = {"announced", "departure-estimate-changed", "arrival-estimate-changed", "departed",
        "landed", "removed"})
    void aFlightEventIsPassedOnWithTheFieldsOfItsExample(String name) {
        FlightEvent event = flightEvents.read(example("flight-operations.flight." + name));
        String type = MessageTypes.forwardedTypeOf(event);

        FlightEventJson published = FlightEventJson.from(event, UUID.randomUUID(), type);

        assertThat(type).isEqualTo("messaging.flight." + name);
        assertThat(published.occurredOn()).isEqualTo(event.occurredOn());
        assertThat(fieldsOf(MAPPER.writeValueAsString(published))).isEqualTo(fieldsOf(example(type)));
    }

    /** OutboxWriter publishes this record for every flight of an SSM. */
    @Test
    void aScheduledFlightIsPublishedWithTheFieldsOfItsExample() {
        FlightScheduledJson published = new FlightScheduledJson(UUID.randomUUID(), MessageTypes.FLIGHT_SCHEDULED,
            MessageTypes.VERSION, Instant.parse("2026-01-02T08:59:59Z"), "HJ221", "BRU", "ATH", "SXBAA",
            Instant.parse("2026-01-09T06:30:00Z"), Instant.parse("2026-01-09T09:45:00Z"));

        assertThat(fieldsOf(MAPPER.writeValueAsString(published)))
            .isEqualTo(fieldsOf(example(MessageTypes.FLIGHT_SCHEDULED)));
    }

    /** OutboxWriter publishes this record for every ULD of a ULD message. */
    @Test
    void aBookedUldIsPublishedWithTheFieldsOfItsExample() {
        UldBookingReceivedJson published = new UldBookingReceivedJson(UUID.randomUUID(),
            MessageTypes.ULD_BOOKING_RECEIVED, MessageTypes.VERSION, Instant.parse("2026-01-05T08:00:00Z"), "HJ221",
            LocalDate.of(2026, 1, 9), "BRU", "AKE16689R7", new BigDecimal("1250"), "KG", "ATH");

        assertThat(fieldsOf(MAPPER.writeValueAsString(published)))
            .isEqualTo(fieldsOf(example(MessageTypes.ULD_BOOKING_RECEIVED)));
    }
}
