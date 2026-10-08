package be.kdg.airfreight.flightoperations.infrastructure.reference;

import be.kdg.airfreight.flightoperations.domain.InvalidValueException;
import be.kdg.airfreight.flightoperations.domain.flight.DelayCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResourceDelayCodeCatalogTest {
    private final ResourceDelayCodeCatalog catalog = new ResourceDelayCodeCatalog();

    @Test
    void theListHoldsTheIataDelayReasonsInOrder() {
        assertThat(catalog.all()).isNotEmpty().isSortedAccordingTo(
            (first, second) -> first.code().value().compareTo(second.code().value()));
        assertThat(catalog.requireKnown(new DelayCode("93")).description()).isEqualTo("Aircraft rotation");
    }

    @Test
    void aCodeThatIataDoesNotUseIsNotInTheList() {
        assertThatThrownBy(() -> catalog.requireKnown(new DelayCode("50")))
            .isInstanceOf(InvalidValueException.class);
    }
}
