package be.kdg.airfreight.flightoperations.infrastructure.messaging;

import be.kdg.airfreight.flightoperations.domain.flight.ActualTimeTolerance;
import be.kdg.airfreight.flightoperations.domain.flight.AircraftRegistration;
import be.kdg.airfreight.flightoperations.domain.flight.AirportCode;
import be.kdg.airfreight.flightoperations.domain.flight.DelayCode;
import be.kdg.airfreight.flightoperations.domain.flight.DelayCodeCatalog;
import be.kdg.airfreight.flightoperations.domain.flight.DelayDuration;
import be.kdg.airfreight.flightoperations.domain.flight.DelayKind;
import be.kdg.airfreight.flightoperations.domain.flight.DelayReason;
import be.kdg.airfreight.flightoperations.domain.flight.Flight;
import be.kdg.airfreight.flightoperations.domain.flight.FlightDeparted;
import be.kdg.airfreight.flightoperations.domain.flight.FlightEvent;
import be.kdg.airfreight.flightoperations.domain.flight.FlightLanded;
import be.kdg.airfreight.flightoperations.domain.flight.FlightNumber;
import be.kdg.airfreight.flightoperations.domain.flight.ScheduledTimes;
import be.kdg.airfreight.flightoperations.domain.flightplanner.FlightPlannerId;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static be.kdg.airfreight.flightoperations.Contracts.example;
import static be.kdg.airfreight.flightoperations.Contracts.fieldsOf;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Flight Operations against the example messages in contracts/. The flight events it publishes must have exactly
 * the fields of their example, because Messaging tests its reader with the same examples. The message from
 * Messaging that the CPM is written must be readable here.
 */
class ContractTest {
    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final Instant STD = Instant.parse("2026-01-09T06:45:00Z");
    private static final Instant STA = Instant.parse("2026-01-09T10:00:00Z");
    private static final Instant NOW = Instant.parse("2026-01-09T05:00:00Z");
    private static final DelayCode REASON = new DelayCode("93");
    private static final DelayCodeCatalog DELAY_CODES = () -> List.of(new DelayReason(REASON, "Aircraft rotation"));
    private static final ActualTimeTolerance TOLERANCE = new ActualTimeTolerance(Duration.ofMinutes(5));

    private final FlightPlannerId planner = FlightPlannerId.create();
    private final Flight flight = Flight.plan(new FlightNumber("HJ221"), new AirportCode("BRU"),
        new AirportCode("ATH"), new AircraftRegistration("SXBAA"), new ScheduledTimes(STD, STA));

    @Test
    void theEventsOfADepartureDelayArePublishedWithTheFieldsOfTheirExamples() {
        List<FlightEvent> events = flight.addDelay(DelayKind.DEPARTURE, new DelayDuration(15), REASON,
            "Aircraft arrived late", planner, NOW, DELAY_CODES);

        assertPublishedAsItsExample(events.get(0), "flight-operations.flight.departure-estimate-changed");
        assertPublishedAsItsExample(events.get(1), "flight-operations.flight.arrival-estimate-changed");
    }

    @Test
    void aDepartureAndAnArrivalArePublishedWithTheFieldsOfTheirExamples() {
        flight.recordFinalizedLoad(NOW);
        FlightDeparted departed = flight.registerDeparture(STD, planner, STD, TOLERANCE);
        FlightLanded landed = flight.registerArrival(STA, planner, STA, TOLERANCE);

        assertPublishedAsItsExample(departed, "flight-operations.flight.departed");
        assertPublishedAsItsExample(landed, "flight-operations.flight.landed");
    }

    @Test
    void theMessageThatTheCpmIsWrittenCanBeRead() {
        CpmCreatedJson json = MAPPER.readValue(example("messaging.cpm.created"), CpmCreatedJson.class);

        assertThat(json.messageId()).isNotNull();
        assertThat(json.type()).isEqualTo(MessagingConfig.CPM_CREATED);
        assertThat(json.flightNumber()).isEqualTo("HJ221");
        assertThat(json.scheduledDepartureDate()).isEqualTo(LocalDate.of(2026, 1, 9));
        assertThat(json.departureAirport()).isEqualTo("BRU");
    }

    private static void assertPublishedAsItsExample(FlightEvent event, String type) {
        FlightEventJson published = FlightEventJson.from(event, UUID.randomUUID()).orElseThrow();

        assertThat(published.type()).isEqualTo(type);
        assertThat(fieldsOf(MAPPER.writeValueAsString(published))).isEqualTo(fieldsOf(example(type)));
    }
}
