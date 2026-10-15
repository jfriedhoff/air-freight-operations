package be.kdg.airfreight.flightoperations.domain.flight;

import be.kdg.airfreight.flightoperations.domain.flightplanner.FlightPlannerId;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The events that Flight returns. The Cucumber scenarios check the state of Flight; these tests check what
 * the application service publishes and Messaging turns into MVT messages.
 */
class FlightTest {
    private static final Instant STD = Instant.parse("2026-01-09T06:45:00Z");
    private static final Instant STA = Instant.parse("2026-01-09T10:00:00Z");
    private static final Instant NOW = Instant.parse("2026-01-09T05:00:00Z");
    private static final ActualTimeTolerance TOLERANCE = new ActualTimeTolerance(Duration.ofMinutes(5));
    private static final DelayCode REASON = new DelayCode("93");
    private static final String DESCRIPTION = "Aircraft arrived late";
    private static final DelayCodeCatalog DELAY_CODES = () -> List.of(new DelayReason(REASON, "Aircraft rotation"));

    private final FlightPlannerId planner = FlightPlannerId.create();
    private final Flight flight = Flight.plan(new FlightNumber("HJ221"), new AirportCode("BRU"),
        new AirportCode("ATH"), new AircraftRegistration("SXBAA"), new ScheduledTimes(STD, STA));

    @Test
    void aDepartureDelayAnnouncesTheNewEtdAndEta() {
        List<FlightEvent> events = flight.addDelay(DelayKind.DEPARTURE, new DelayDuration(15), REASON,
            DESCRIPTION, planner, NOW, DELAY_CODES);

        assertThat(events).satisfiesExactly(
            event -> assertThat(event).isInstanceOfSatisfying(DepartureEstimateChanged.class, changed -> {
                assertThat(changed.etd()).isEqualTo(STD.plus(Duration.ofMinutes(15)));
                assertThat(changed.changedBy()).isEqualTo(planner);
            }),
            event -> assertThat(event).isInstanceOfSatisfying(ArrivalEstimateChanged.class, changed ->
                assertThat(changed.eta()).isEqualTo(STA.plus(Duration.ofMinutes(15)))));
    }

    @Test
    void anArrivalDelayOnlyAnnouncesTheNewEta() {
        List<FlightEvent> events = flight.addDelay(DelayKind.ARRIVAL, new DelayDuration(20), REASON,
            DESCRIPTION, planner, NOW, DELAY_CODES);

        assertThat(events).satisfiesExactly(
            event -> assertThat(event).isInstanceOfSatisfying(ArrivalEstimateChanged.class, changed ->
                assertThat(changed.eta()).isEqualTo(STA.plus(Duration.ofMinutes(20)))));
    }

    @Test
    void changingTheMinutesOfADelayAnnouncesTheNewEstimates() {
        DelayId delay = addDepartureDelay(15);

        List<FlightEvent> events = flight.changeDelay(delay, new DelayDuration(30), DESCRIPTION, planner, NOW);

        assertThat(events).hasSize(2).first().isInstanceOfSatisfying(DepartureEstimateChanged.class, changed ->
            assertThat(changed.etd()).isEqualTo(STD.plus(Duration.ofMinutes(30))));
    }

    @Test
    void changingOnlyTheDescriptionOfADelayAnnouncesNothing() {
        DelayId delay = addDepartureDelay(15);

        List<FlightEvent> events = flight.changeDelay(delay, new DelayDuration(15), "Crew arrived late",
            planner, NOW);

        assertThat(events).isEmpty();
    }

    /** FinalizedLoadService asks isFinalizedLoadReceived to ignore a second CPM: no scenario reads it. */
    @Test
    void receivingTheFinalizedLoadIsAnnounced() {
        FinalizedLoadReceived event = flight.recordFinalizedLoad(NOW);

        assertThat(event.flightId()).isEqualTo(flight.getId());
        assertThat(event.occurredOn()).isEqualTo(NOW);
        assertThat(flight.isFinalizedLoadReceived()).isTrue();
    }

    @Test
    void registeringTheAtdAnnouncesTheDeparture() {
        flight.recordFinalizedLoad(NOW);

        FlightDeparted event = flight.registerDeparture(STD, planner, STD, TOLERANCE);

        assertThat(event.flightId()).isEqualTo(flight.getId());
        assertThat(event.atd()).isEqualTo(STD);
        assertThat(event.registeredBy()).isEqualTo(planner);
    }

    @Test
    void registeringTheAtaAnnouncesTheLanding() {
        flight.recordFinalizedLoad(NOW);
        flight.registerDeparture(STD, planner, STD, TOLERANCE);

        FlightLanded event = flight.registerArrival(STA, planner, STA, TOLERANCE);

        assertThat(event.flightId()).isEqualTo(flight.getId());
        assertThat(event.ata()).isEqualTo(STA);
        assertThat(event.registeredBy()).isEqualTo(planner);
    }

    private DelayId addDepartureDelay(int minutes) {
        flight.addDelay(DelayKind.DEPARTURE, new DelayDuration(minutes), REASON, DESCRIPTION, planner, NOW,
            DELAY_CODES);
        return flight.getDelays().getLast().getId();
    }
}
