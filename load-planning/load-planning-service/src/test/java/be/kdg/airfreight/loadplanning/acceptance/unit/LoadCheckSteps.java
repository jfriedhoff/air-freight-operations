package be.kdg.airfreight.loadplanning.acceptance.unit;

import be.kdg.airfreight.loadplanning.domain.flightcargo.CheckSeverity;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadCheckResult;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/** All load checks together and their report. The steps of one check are in the class of that check. */
public class LoadCheckSteps {
    private final CargoWorld world;

    public LoadCheckSteps(CargoWorld world) {
        this.world = world;
    }

    @When("the load is checked")
    @When("I click Check Load")
    public void theLoadIsChecked() {
        world.checkAll();
    }

    @Then("the check has passed")
    public void theCheckHasPassed() {
        assertPassed(world.checkResult);
    }

    @Then("the check has failed")
    public void theCheckHasFailed() {
        assertFailed(world.checkResult);
    }

    /** Also selects that check, for the Then steps that read its reason. */
    @Then("the soft check {string} has passed")
    public void theSoftCheckHasPassed(String name) {
        world.checkResult = softCheckNamed(name);
        assertPassed(world.checkResult);
    }

    /** Also selects that check, for the Then steps that read its reason. */
    @Then("the soft check {string} has failed")
    public void theSoftCheckHasFailed(String name) {
        world.checkResult = softCheckNamed(name);
        assertFailed(world.checkResult);
    }

    /** The whole table at once, in the order of the checks, so a missing or extra check fails too. */
    @Then("I see this report:")
    public void iSeeThisReport(DataTable expected) {
        List<List<String>> rows = world.report.results().stream()
            .map(LoadCheckSteps::row)
            .toList();
        assertThat(rows).containsExactlyElementsOf(withoutHeader(expected));
    }

    private static void assertPassed(LoadCheckResult result) {
        assertThat(result.passed()).withFailMessage("The check failed: %s", result.reason()).isTrue();
    }

    private static void assertFailed(LoadCheckResult result) {
        assertThat(result.passed()).withFailMessage("The check passed").isFalse();
    }

    private LoadCheckResult softCheckNamed(String name) {
        LoadCheckResult result = world.report.results().stream()
            .filter(candidate -> candidate.checkName().equalsIgnoreCase(name))
            .findFirst()
            .orElseThrow(() -> new AssertionError("The report has no check \"" + name + "\""));
        assertThat(result.severity()).as("severity of \"%s\"", name).isEqualTo(CheckSeverity.SOFT);
        return result;
    }

    /** A result as a row of the table: check, result, reason, type. */
    private static List<String> row(LoadCheckResult result) {
        return List.of(result.checkName(), result.passed() ? "passed" : "failed",
            Objects.requireNonNullElse(result.reason(), ""), result.severity().name().toLowerCase(Locale.ROOT));
    }

    /** An empty cell is null in a DataTable; the report has no reason there. */
    private static List<List<String>> withoutHeader(DataTable table) {
        return table.asLists().stream()
            .skip(1)
            .map(cells -> cells.stream().map(cell -> Objects.requireNonNullElse(cell, "")).toList())
            .toList();
    }
}
