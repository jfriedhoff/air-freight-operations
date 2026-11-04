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
 * Load Planning against the example messages in contracts/. The finalized load it publishes must have exactly the
 * fields of its example, because Messaging tests its reader with the same example. The flight events and ULD
 * bookings that Messaging publishes must be readable here: MessagingEventListener reads them into these records.
 */
class ContractTest {
    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final String LOAD_FINALIZED = "load-planning.flight-cargo.load-finalized";
    private static final LocalDate JANUARY_9 = LocalDate.of(2026, 1, 9);

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

    @Test
    void anAnnouncedFlightCanBeRead() {
        FlightEventJson json = flightEvent(MessagingConfig.FLIGHT_ANNOUNCED);

        assertThat(json.arrivalAirport()).isEqualTo("ATH");
        assertThat(json.std()).isEqualTo(Instant.parse("2026-01-09T06:30:00Z"));
        assertThat(json.sta()).isEqualTo(Instant.parse("2026-01-09T09:45:00Z"));
    }

    /** occurredOn is when the delay was entered in Flight Operations: it decides whether the period was open. */
    @Test
    void aNewEtdAndEtaCanBeRead() {
        FlightEventJson etd = flightEvent(MessagingConfig.DEPARTURE_ESTIMATE_CHANGED);
        FlightEventJson eta = flightEvent(MessagingConfig.ARRIVAL_ESTIMATE_CHANGED);

        assertThat(etd.etd()).isEqualTo(Instant.parse("2026-01-09T06:45:00Z"));
        assertThat(etd.occurredOn()).isEqualTo(Instant.parse("2026-01-08T12:00:00Z"));
        assertThat(eta.eta()).isEqualTo(Instant.parse("2026-01-09T10:00:00Z"));
    }

    /** These three only need to say which flight they are about. */
    @Test
    void aDepartureALandingAndARemovalCanBeRead() {
        flightEvent(MessagingConfig.FLIGHT_DEPARTED);
        flightEvent(MessagingConfig.FLIGHT_LANDED);
        flightEvent(MessagingConfig.FLIGHT_REMOVED);
    }

    @Test
    void aBookedUldCanBeRead() {
        UldBookingReceivedJson json = MAPPER.readValue(example(MessagingConfig.ULD_BOOKING_RECEIVED),
            UldBookingReceivedJson.class);

        assertThat(json.messageId()).isNotNull();
        assertThat(json.type()).isEqualTo(MessagingConfig.ULD_BOOKING_RECEIVED);
        assertThat(json.flightNumber()).isEqualTo("HJ221");
        assertThat(json.scheduledDepartureDate()).isEqualTo(JANUARY_9);
        assertThat(json.departureAirport()).isEqualTo("BRU");
        assertThat(json.uldCode()).isEqualTo("AKE16689R7");
        assertThat(json.weight()).isEqualByComparingTo("1250");
        assertThat(json.weightUnit()).isEqualTo("KG");
        assertThat(json.destination()).isEqualTo("ATH");
    }

    /** Reads the example of a flight event and checks the fields that every flight event has. */
    private static FlightEventJson flightEvent(String type) {
        FlightEventJson json = MAPPER.readValue(example(type), FlightEventJson.class);

        assertThat(json.messageId()).isNotNull();
        assertThat(json.type()).isEqualTo(type);
        assertThat(json.occurredOn()).isNotNull();
        assertThat(json.flightNumber()).isEqualTo("HJ221");
        assertThat(json.scheduledDepartureDate()).isEqualTo(JANUARY_9);
        assertThat(json.departureAirport()).isEqualTo("BRU");
        return json;
    }
}
