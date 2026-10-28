package be.kdg.airfreight.messaging.acceptance.unit;

import be.kdg.airfreight.messaging.domain.AircraftRegistration;
import be.kdg.airfreight.messaging.domain.AirportCatalog;
import be.kdg.airfreight.messaging.domain.AirportCode;
import be.kdg.airfreight.messaging.domain.DomainRuleViolationException;
import be.kdg.airfreight.messaging.domain.FlightNumber;
import be.kdg.airfreight.messaging.domain.FormatVersion;
import be.kdg.airfreight.messaging.domain.MessageId;
import be.kdg.airfreight.messaging.domain.booking.UldCode;
import be.kdg.airfreight.messaging.domain.booking.UldMessage;
import be.kdg.airfreight.messaging.domain.booking.WeightUnit;
import be.kdg.airfreight.messaging.domain.schedule.FlightSeries;
import be.kdg.airfreight.messaging.domain.schedule.SchedulePeriod;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Story 29. "A ULD message is read" builds its values as JsonUldMessageReader does after reading the JSON: the
 * ULD code, the weight and its unit, the destination from the IATA list and the format version. A refused value
 * makes the message invalid, and BookingService sets it aside. "An SSM is read" does the same for the airports
 * of a flight series. Reading the JSON and setting the message aside are tested by the integration tests.
 * What a scenario does not name gets a valid value.
 */
public class IncomingMessageSteps {
    private static final AirportCatalog AIRPORTS = Set.of(new AirportCode("BRU"), new AirportCode("ATH"))::contains;
    private static final LocalDate MONDAY = LocalDate.of(2026, 1, 5);

    private Throwable error;

    @When("a ULD message is read with ULD code {word}")
    public void withUldCode(String uldCode) {
        readUldMessage(uldCode, new BigDecimal("1250"), "KG", "ATH", 1);
    }

    @When("a ULD message is read with weight {bigdecimal} kg")
    public void withWeight(BigDecimal weight) {
        readUldMessage("AKE16689R7", weight, "KG", "ATH", 1);
    }

    @When("a ULD message is read with weight unit {word}")
    public void withWeightUnit(String unit) {
        readUldMessage("AKE16689R7", new BigDecimal("1250"), unit, "ATH", 1);
    }

    @When("a ULD message is read with destination {word}, which is not in the IATA list")
    public void withDestination(String destination) {
        readUldMessage("AKE16689R7", new BigDecimal("1250"), "KG", destination, 1);
    }

    @When("a ULD message is read with format version {int}")
    public void withFormatVersion(int version) {
        readUldMessage("AKE16689R7", new BigDecimal("1250"), "KG", "ATH", version);
    }

    @When("an SSM is read for a flight from {word} to {word}")
    public void anSsmIsRead(String from, String to) {
        error = catchThrowable(() -> new FlightSeries(new SchedulePeriod(MONDAY, MONDAY), Set.of(DayOfWeek.MONDAY),
            LocalTime.of(6, 30), LocalTime.of(9, 45), new FlightNumber("HJ221"), AIRPORTS.airport(from),
            AIRPORTS.airport(to), new AircraftRegistration("SXBAA")));
    }

    @Then("the message is valid")
    public void theMessageIsValid() {
        assertThat(error).isNull();
    }

    @Then("the message is invalid because {string}")
    public void theMessageIsInvalidBecause(String reason) {
        assertThat(error).isInstanceOf(DomainRuleViolationException.class).hasMessageContaining(reason);
    }

    private void readUldMessage(String uldCode, BigDecimal weight, String unit, String destination, int version) {
        error = catchThrowable(() -> new UldMessage(MessageId.create(), new FormatVersion(version),
            new FlightNumber("HJ221"), 9, AIRPORTS.airport("BRU"), new UldCode(uldCode), weight,
            WeightUnit.of(unit), AIRPORTS.airport(destination)));
    }
}
