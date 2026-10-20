package be.kdg.airfreight.loadplanning.application.query;

import java.time.LocalDate;

import be.kdg.airfreight.loadplanning.domain.InvalidValueException;
import be.kdg.airfreight.loadplanning.domain.flightcargo.AirportCode;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightNumber;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** A flight is found by its flight number and departure airport together: half an identity names no flight. */
class FlightsToLoadQueryTest {
    private static final LocalDate DAY = LocalDate.of(2026, 1, 9);

    @Test
    void aDayAloneAsksForTheFlightsToLoadOfThatDay() {
        assertThat(new FlightsToLoadQuery(DAY, null, null, false).byIdentity()).isFalse();
    }

    @Test
    void flightNumberAndDepartureAirportTogetherAskForOneFlight() {
        assertThat(new FlightsToLoadQuery(DAY, new FlightNumber("HJ221"), new AirportCode("BRU"), true).byIdentity())
            .isTrue();
    }

    @Test
    void aFlightNumberWithoutDepartureAirportIsRefused() {
        assertThatThrownBy(() -> new FlightsToLoadQuery(DAY, new FlightNumber("HJ221"), null, true))
            .isInstanceOf(InvalidValueException.class)
            .hasMessageContaining("both or neither");
    }

    @Test
    void aDepartureAirportWithoutFlightNumberIsRefused() {
        assertThatThrownBy(() -> new FlightsToLoadQuery(DAY, null, new AirportCode("BRU"), true))
            .isInstanceOf(InvalidValueException.class)
            .hasMessageContaining("both or neither");
    }
}
