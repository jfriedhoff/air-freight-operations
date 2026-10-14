package be.kdg.airfreight.messaging.domain;

import be.kdg.airfreight.messaging.domain.booking.UldCode;
import be.kdg.airfreight.messaging.domain.booking.WeightUnit;
import be.kdg.airfreight.messaging.domain.schedule.FlightSeries;
import be.kdg.airfreight.messaging.domain.schedule.SchedulePeriod;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The values of an incoming message that story 29 refuses: ULD code, weight unit and airports. */
class IncomingMessageRulesTest {
    private static final AirportCode BRU = new AirportCode("BRU");
    private static final AirportCode ATH = new AirportCode("ATH");
    private static final AirportCatalog AIRPORTS = Set.of(BRU, ATH)::contains;
    private static final LocalDate MONDAY = LocalDate.of(2026, 1, 5);

    @Test
    void aUldCodeHasThreeLettersASerialNumberAndAnOwnerCode() {
        assertThat(new UldCode("AKE16689R7").value()).isEqualTo("AKE16689R7");
        assertThat(new UldCode("PMC1234KL").value()).isEqualTo("PMC1234KL");

        assertThatThrownBy(() -> new UldCode("AK16689R7"))
            .isInstanceOf(DomainRuleViolationException.class)
            .hasMessageContaining("ULD code 'AK16689R7' is invalid");
        assertThatThrownBy(() -> new UldCode("ake16689R7")).isInstanceOf(DomainRuleViolationException.class);
        assertThatThrownBy(() -> new UldCode("")).isInstanceOf(DomainRuleViolationException.class);
    }

    @Test
    void aWeightIsInKilogramsOrPounds() {
        assertThat(WeightUnit.of("KG")).isEqualTo(WeightUnit.KG);
        assertThat(WeightUnit.of("LB")).isEqualTo(WeightUnit.LB);

        assertThatThrownBy(() -> WeightUnit.of("T"))
            .isInstanceOf(DomainRuleViolationException.class)
            .hasMessage("Weight unit 'T' is invalid: expected KG or LB");
        assertThatThrownBy(() -> WeightUnit.of("kg")).isInstanceOf(DomainRuleViolationException.class);
    }

    @Test
    void anAirportMustBeInTheIataList() {
        assertThat(AIRPORTS.airport("BRU")).isEqualTo(BRU);

        assertThatThrownBy(() -> AIRPORTS.airport("QQQ"))
            .isInstanceOf(DomainRuleViolationException.class)
            .hasMessage("Airport 'QQQ' is not in the IATA list");
    }

    @Test
    void aFlightSeriesCannotDepartFromAndArriveAtTheSameAirport() {
        assertThat(seriesBetween(BRU, ATH).arrivalAirport()).isEqualTo(ATH);

        assertThatThrownBy(() -> seriesBetween(BRU, BRU))
            .isInstanceOf(DomainRuleViolationException.class)
            .hasMessageContaining("the same airport (BRU)");
    }

    private static FlightSeries seriesBetween(AirportCode departure, AirportCode arrival) {
        return new FlightSeries(new SchedulePeriod(MONDAY, MONDAY), Set.of(DayOfWeek.MONDAY), LocalTime.of(6, 30),
            LocalTime.of(9, 45), new FlightNumber("HJ221"), departure, arrival, new AircraftRegistration("SXBAA"));
    }
}
