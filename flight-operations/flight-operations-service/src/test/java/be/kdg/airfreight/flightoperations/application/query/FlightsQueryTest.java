package be.kdg.airfreight.flightoperations.application.query;

import java.time.LocalDate;

import be.kdg.airfreight.flightoperations.domain.InvalidValueException;
import be.kdg.airfreight.flightoperations.domain.flight.AirportCode;
import be.kdg.airfreight.flightoperations.domain.flight.FlightNumber;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** A flight is found by its flight number and departure airport together: half an identity names no flight. */
class FlightsQueryTest {
    private static final LocalDate DAY = LocalDate.of(2026, 1, 9);

    @Test
    void aDayAloneAsksForTheFlightsOfThatDay() {
        assertThat(new FlightsQuery(DAY, null, null).byIdentity()).isFalse();
    }

    @Test
    void withoutADayTheQueryLeavesTheDayToTheService() {
        assertThat(new FlightsQuery(null, null, null).scheduledDepartureDate()).isNull();
    }

    @Test
    void flightNumberAndDepartureAirportTogetherAskForOneFlight() {
        assertThat(new FlightsQuery(DAY, new FlightNumber("HJ221"), new AirportCode("BRU")).byIdentity()).isTrue();
    }

    @Test
    void aFlightNumberWithoutDepartureAirportIsRefused() {
        assertThatThrownBy(() -> new FlightsQuery(DAY, new FlightNumber("HJ221"), null))
            .isInstanceOf(InvalidValueException.class)
            .hasMessageContaining("both or neither");
    }

    @Test
    void aDepartureAirportWithoutFlightNumberIsRefused() {
        assertThatThrownBy(() -> new FlightsQuery(DAY, null, new AirportCode("BRU")))
            .isInstanceOf(InvalidValueException.class)
            .hasMessageContaining("both or neither");
    }
}
