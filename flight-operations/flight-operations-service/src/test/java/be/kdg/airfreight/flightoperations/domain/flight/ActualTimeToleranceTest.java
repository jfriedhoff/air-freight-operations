package be.kdg.airfreight.flightoperations.domain.flight;

import be.kdg.airfreight.flightoperations.domain.DomainRuleViolationException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ActualTimeToleranceTest {
    @Test
    void aNegativeToleranceIsRefused() {
        assertThatThrownBy(() -> new ActualTimeTolerance(Duration.ofMinutes(-1)))
            .isInstanceOf(DomainRuleViolationException.class)
            .hasMessageContaining("cannot be negative");
    }

    @Test
    void aToleranceOfZeroIsAllowed() {
        assertThat(new ActualTimeTolerance(Duration.ZERO).value()).isEqualTo(Duration.ZERO);
    }
}
