package be.kdg.airfreight.flightoperations.domain;

import be.kdg.airfreight.flightoperations.domain.flight.AircraftRegistration;
import be.kdg.airfreight.flightoperations.domain.flight.AirportCode;
import be.kdg.airfreight.flightoperations.domain.flight.DelayCode;
import be.kdg.airfreight.flightoperations.domain.flight.DelayCodeCatalog;
import be.kdg.airfreight.flightoperations.domain.flight.DelayDuration;
import be.kdg.airfreight.flightoperations.domain.flight.DelayKind;
import be.kdg.airfreight.flightoperations.domain.flight.DelayReason;
import be.kdg.airfreight.flightoperations.domain.flight.Flight;
import be.kdg.airfreight.flightoperations.domain.flight.FlightNumber;
import be.kdg.airfreight.flightoperations.domain.flight.ScheduledTimes;
import be.kdg.airfreight.flightoperations.domain.flightplanner.FlightPlannerId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** A value that is wrong in itself is told apart from an action that does not fit the state of the flight. */
class InvalidValueTest {
    private static final Instant STD = Instant.parse("2026-01-09T06:30:00Z");
    private static final Instant STA = Instant.parse("2026-01-09T09:45:00Z");
    private static final Instant NOW = Instant.parse("2026-01-09T05:00:00Z");
    private static final DelayCode ROTATION = new DelayCode("93");
    private static final DelayCodeCatalog DELAY_CODES = () -> List.of(new DelayReason(ROTATION, "Aircraft rotation"));

    @Test
    void aDelayOutsideOneMinuteToTwelveHoursIsAnInvalidValue() {
        assertThatThrownBy(() -> new DelayDuration(0)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new DelayDuration(721)).isInstanceOf(InvalidValueException.class);
    }

    @Test
    void aMalformedDelayCodeOrAirportCodeIsAnInvalidValue() {
        assertThatThrownBy(() -> new DelayCode("XY")).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new AirportCode("bru")).isInstanceOf(InvalidValueException.class);
    }

    @Test
    void aDelayWithoutADescriptionIsAnInvalidValue() {
        Flight flight = plannedFlight();

        assertThatThrownBy(() -> flight.addDelay(DelayKind.DEPARTURE, new DelayDuration(15), ROTATION, " ",
            FlightPlannerId.create(), NOW, DELAY_CODES)).isInstanceOf(InvalidValueException.class);
    }

    @Test
    void theFlightRefusesADelayReasonOutsideTheIataList() {
        Flight flight = plannedFlight();

        assertThatThrownBy(() -> flight.addDelay(DelayKind.DEPARTURE, new DelayDuration(15), new DelayCode("50"),
            "Late aircraft", FlightPlannerId.create(), NOW, DELAY_CODES))
            .isInstanceOf(InvalidValueException.class)
            .hasMessage("Delay reason '50' is not in the IATA list");
        assertThat(flight.getDelays()).isEmpty();
    }

    @Test
    void moreThanTwelveHoursOfDelaysTogetherDependsOnTheFlightAndIsNotAnInvalidValue() {
        Flight flight = plannedFlight();
        flight.addDelay(DelayKind.DEPARTURE, new DelayDuration(700), ROTATION, "Late aircraft",
            FlightPlannerId.create(), NOW, DELAY_CODES);

        assertThatThrownBy(() -> flight.addDelay(DelayKind.DEPARTURE, new DelayDuration(21), ROTATION,
            "Late crew", FlightPlannerId.create(), NOW, DELAY_CODES))
            .isExactlyInstanceOf(DomainRuleViolationException.class);
    }

    private static Flight plannedFlight() {
        return Flight.plan(new FlightNumber("HJ221"), new AirportCode("BRU"), new AirportCode("ATH"),
            new AircraftRegistration("SXBAA"), new ScheduledTimes(STD, STA));
    }
}
