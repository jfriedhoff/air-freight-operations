package be.kdg.airfreight.flightoperations;

import be.kdg.airfreight.flightoperations.application.FlightRetentionService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * The daily job at 00:00 UTC is missed when Flight Operations is not running at midnight, so
 * FlightRetentionScheduler also removes the expired flights once when the unit starts. The rule itself is
 * tested in story #38.
 */
@SpringBootTest
class FlightRetentionAtStartupTest {
    @MockitoBean
    private FlightRetentionService retention;

    @Test
    void expiredFlightsAreRemovedOnceWhenFlightOperationsStarts() {
        verify(retention, times(1)).removeExpiredFlights();
    }
}
