package be.kdg.airfreight.loadplanning.domain.flightcargo;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** A ULD is identified by its code: weight and destination do not matter. */
class UldTest {
    private static final Weight WEIGHT = new Weight(new BigDecimal("1000"), WeightUnit.KG);
    private static final AirportCode ATH = new AirportCode("ATH");

    @Test
    void twoUldsWithTheSameCodeAreTheSameUld() {
        Uld uld = new Uld(new UldCode("AKE16689R7"), WEIGHT, ATH);
        Uld sameCode = new Uld(new UldCode("AKE16689R7"), new Weight(new BigDecimal("1400"), WeightUnit.KG),
            new AirportCode("FRA"));

        assertThat(uld).isEqualTo(sameCode).hasSameHashCodeAs(sameCode);
    }

    @Test
    void anotherCodeIsAnotherUld() {
        assertThat(new Uld(new UldCode("AKE16689R7"), WEIGHT, ATH))
            .isNotEqualTo(new Uld(new UldCode("AKE20417HJ"), WEIGHT, ATH));
    }

    @Test
    void aUldIsNotEqualToSomethingElse() {
        assertThat(new Uld(new UldCode("AKE16689R7"), WEIGHT, ATH)).isNotEqualTo("AKE16689R7");
    }
}
