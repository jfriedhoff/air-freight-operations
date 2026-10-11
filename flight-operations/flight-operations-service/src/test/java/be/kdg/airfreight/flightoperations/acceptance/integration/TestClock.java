package be.kdg.airfreight.flightoperations.acceptance.integration;

import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * The clock of the integration scenarios, used instead of the system clock of PolicyConfig: the system
 * time, unless a scenario sets the time ("it is 16 January 2026 00:00 UTC"). Time is outside our code.
 */
@Primary
public class TestClock extends Clock {
    private @Nullable Instant fixed;

    void set(Instant now) {
        fixed = now;
    }

    void reset() {
        fixed = null;
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
