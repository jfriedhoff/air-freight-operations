package be.kdg.airfreight.loadplanning.acceptance.integration;

import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * A clock that a scenario can set, used instead of the system clock of PolicyConfig in the unit that
 * story 37 restarts: the system time, unless the scenario sets the time. Time is outside our code.
 */
@Primary
public class TestClock extends Clock {
    private @Nullable Instant fixed;

    void set(Instant now) {
        fixed = now;
    }

    @Override
    public Instant instant() {
        return fixed != null ? fixed : Instant.now();
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }
}
