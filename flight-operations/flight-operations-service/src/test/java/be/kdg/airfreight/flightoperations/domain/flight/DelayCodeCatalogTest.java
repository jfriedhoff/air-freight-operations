package be.kdg.airfreight.flightoperations.domain.flight;

import be.kdg.airfreight.flightoperations.domain.InvalidValueException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** What the catalog returns and what an entry needs. Refusing an unknown reason is story 08. */
class DelayCodeCatalogTest {
    private static final DelayReason ROTATION = new DelayReason(new DelayCode("93"), "Aircraft rotation");
    /** The catalog has one method without a body, so a lambda is a catalog. */
    private final DelayCodeCatalog catalog = () -> List.of(ROTATION);

    @Test
    void aDelayReasonFromTheIataListIsKnown() {
        assertThat(catalog.requireKnown(new DelayCode("93"))).isEqualTo(ROTATION);
    }

    /** The entry of the list itself; the description a planner gives a delay is story 08. */
    @Test
    void anEntryOfTheIataListNeedsADescription() {
        assertThatThrownBy(() -> new DelayReason(new DelayCode("93"), " "))
            .isInstanceOf(InvalidValueException.class);
    }
}
