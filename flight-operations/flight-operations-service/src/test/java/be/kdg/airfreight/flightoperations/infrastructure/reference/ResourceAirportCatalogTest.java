package be.kdg.airfreight.flightoperations.infrastructure.reference;

import be.kdg.airfreight.flightoperations.domain.flight.AirportCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceAirportCatalogTest {
    private final ResourceAirportCatalog catalog = new ResourceAirportCatalog();

    @Test
    void anAirportInTheListHasAName() {
        assertThat(catalog.nameOf(new AirportCode("BRU"))).contains("Brussels Airport");
        assertThat(catalog.nameOf(new AirportCode("ATH"))).contains("Athens International Airport");
    }

    @Test
    void anAirportOutsideTheListHasNoName() {
        assertThat(catalog.nameOf(new AirportCode("ZZZ"))).isEmpty();
    }
}
