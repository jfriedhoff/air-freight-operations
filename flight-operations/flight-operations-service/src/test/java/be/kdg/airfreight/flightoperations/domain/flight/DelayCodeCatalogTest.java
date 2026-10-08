package be.kdg.airfreight.flightoperations.domain.flight;

import be.kdg.airfreight.flightoperations.domain.InvalidValueException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DelayCodeCatalogTest {
    private static final DelayReason ROTATION = new DelayReason(new DelayCode("93"), "Aircraft rotation");
    /** The catalog has one method without a body, so a lambda is a catalog. */
    private final DelayCodeCatalog catalog = () -> List.of(ROTATION);

    @Test
    void aDelayReasonFromTheIataListIsKnown() {
        assertThat(catalog.requireKnown(new DelayCode("93"))).isEqualTo(ROTATION);
    }

    @Test
    void aDelayReasonOutsideTheIataListIsRefusedWithAnExplanation() {
        assertThatThrownBy(() -> catalog.requireKnown(new DelayCode("50")))
            .isInstanceOf(InvalidValueException.class)
            .hasMessage("Delay reason '50' is not in the IATA list");
    }

    @Test
    void aDelayReasonNeedsADescription() {
        assertThatThrownBy(() -> new DelayReason(new DelayCode("93"), " "))
            .isInstanceOf(InvalidValueException.class);
    }
}
