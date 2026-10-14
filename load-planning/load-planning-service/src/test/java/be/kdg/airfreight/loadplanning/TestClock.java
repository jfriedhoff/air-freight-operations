package be.kdg.airfreight.loadplanning;

import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * A clock that a test can set, used instead of the system clock of PolicyConfig: the system time, unless the
 * test sets the time. Every Spring test imports it, so they all share one application context; a test that
 * sets the time resets it afterwards. Time is outside our code.
 */
@Primary
public class TestClock extends Clock {
    private @Nullable Instant fixed;

    public void set(Instant now) {
        fixed = now;
    }

    public void reset() {
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
