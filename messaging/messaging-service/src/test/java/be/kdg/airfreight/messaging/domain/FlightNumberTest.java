package be.kdg.airfreight.messaging.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** A flight number is an airline code with at least one letter, followed by one to four digits. */
class FlightNumberTest {

    @Test
    void anAirlineCodeOfTwoCharactersWithALetterOrOfThreeLetters() {
        assertThat(new FlightNumber("HJ221").value()).isEqualTo("HJ221");
        assertThat(new FlightNumber("3V11").value()).isEqualTo("3V11");
        assertThat(new FlightNumber("A1234").value()).isEqualTo("A1234");
        assertThat(new FlightNumber("ABC123").value()).isEqualTo("ABC123");
    }

    /** The longest flight number has seven characters; the database columns that store it are as wide. */
    @Test
    void aThreeLetterCodeWithFourDigitsIsTheLongestFlightNumber() {
        assertThat(new FlightNumber("ABC1234").value()).isEqualTo("ABC1234");
        assertThatThrownBy(() -> new FlightNumber("ABCD123")).isInstanceOf(DomainRuleViolationException.class);
        assertThatThrownBy(() -> new FlightNumber("ABC12345")).isInstanceOf(DomainRuleViolationException.class);
    }

    @Test
    void anAirlineCodeWithoutALetterIsInvalid() {
        assertThatThrownBy(() -> new FlightNumber("221"))
            .isInstanceOf(DomainRuleViolationException.class)
            .hasMessageContaining("Flight number '221' is invalid");
        assertThatThrownBy(() -> new FlightNumber("22221")).isInstanceOf(DomainRuleViolationException.class);
    }

    @Test
    void lowercaseLettersNoDigitsOrMoreThanFourDigitsAreInvalid() {
        assertThatThrownBy(() -> new FlightNumber("hj221")).isInstanceOf(DomainRuleViolationException.class);
        assertThatThrownBy(() -> new FlightNumber("HJ")).isInstanceOf(DomainRuleViolationException.class);
        assertThatThrownBy(() -> new FlightNumber("HJ12345")).isInstanceOf(DomainRuleViolationException.class);
    }
}
