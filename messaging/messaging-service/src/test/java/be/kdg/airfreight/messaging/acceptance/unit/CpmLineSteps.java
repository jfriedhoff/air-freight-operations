package be.kdg.airfreight.messaging.acceptance.unit;

import be.kdg.airfreight.messaging.domain.AirportCode;
import be.kdg.airfreight.messaging.domain.DomainRuleViolationException;
import be.kdg.airfreight.messaging.domain.load.CpmLine;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Story 25. "Load Planning reports a finalized load" builds a line of the CPM as JsonLoadFinalizedReader does
 * after reading the JSON. A refused line makes the whole finalized load invalid: LoadService then sets it aside
 * and writes no CPM. Reading the JSON and writing the CPM are tested by the integration tests.
 */
public class CpmLineSteps {
    private Throwable error;

    @When("Load Planning reports a finalized load with a ULD line {}")
    public void aFinalizedLoadWithAUldLine(String line) {
        error = catchThrowable(() -> lineFor(line));
    }

    @Then("the finalized load is valid")
    public void theFinalizedLoadIsValid() {
        assertThat(error).isNull();
    }

    @Then("the finalized load is invalid")
    public void theFinalizedLoadIsInvalid() {
        assertThat(error).isInstanceOf(DomainRuleViolationException.class)
            .hasMessage("A CPM line needs a ULD code, a position and a weight unit");
    }

    private static CpmLine lineFor(String line) {
        return switch (line) {
            case "AKE16689R7 on CL in KG" -> line("AKE16689R7", "CL", "KG");
            case "without a ULD code" -> line("", "CL", "KG");
            case "without a load position" -> line("AKE16689R7", "", "KG");
            case "without a weight unit" -> line("AKE16689R7", "CL", "");
            default -> throw new IllegalArgumentException("Unknown ULD line: " + line);
        };
    }

    private static CpmLine line(String uldCode, String position, String weightUnit) {
        return new CpmLine(uldCode, position, new BigDecimal("1250"), weightUnit, new AirportCode("ATH"));
    }
}
