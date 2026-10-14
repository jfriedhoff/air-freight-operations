package be.kdg.airfreight.loadplanning.domain.flightcargo;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FlightIdTest {
    @Test
    void aFlightWithoutACargoIsReportedWithWhatIdentifiesIt() {
        FlightId flight = new FlightId(new FlightNumber("HJ221"), LocalDate.of(2026, 1, 9), new AirportCode("BRU"));

        assertThat(flight.cargoNotFound()).hasMessage("No cargo for flight HJ221 of 2026-01-09 from BRU");
    }
}
