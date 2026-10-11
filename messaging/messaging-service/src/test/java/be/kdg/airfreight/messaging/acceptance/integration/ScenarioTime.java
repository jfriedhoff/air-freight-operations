package be.kdg.airfreight.messaging.acceptance.integration;

import io.cucumber.java.Before;

import java.time.Clock;
import java.time.Instant;

import static org.mockito.BDDMockito.given;

/**
 * Clock is a @MockitoBean (SpringCucumberConfig): without this every scenario would start without a time.
 * The time is the one of the stories, a week before the flight; a step can set another one.
 */
public class ScenarioTime {
    static final Instant NOW = Instant.parse("2026-01-02T12:00:00Z");

    private final Clock clock;

    public ScenarioTime(Clock clock) {
        this.clock = clock;
    }

    @Before
    public void itIsTheTimeOfTheStories() {
        given(clock.instant()).willReturn(NOW);
    }
}
