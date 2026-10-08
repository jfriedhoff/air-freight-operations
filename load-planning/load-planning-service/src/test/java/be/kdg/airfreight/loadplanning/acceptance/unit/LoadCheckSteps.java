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

public class LoadCheckSteps {
    private final CargoWorld world;

    public LoadCheckSteps(CargoWorld world) {
        this.world = world;
    }

    @When("the load is checked")
    public void theLoadIsChecked() {
        world.checkAll();
    }

    @When("I click Check Load")
    public void iClickCheckLoad() {
        theLoadIsChecked();
    }

    /** The whole table at once, in the order of the checks, so a missing or extra check fails too. */
    @Then("I see this report:")
    public void iSeeThisReport(DataTable expected) {
        List<List<String>> rows = world.report.results().stream()
            .map(LoadCheckSteps::row)
            .toList();
        assertThat(rows).containsExactlyElementsOf(withoutHeader(expected));
    }

    @Then("the check has passed")
    public void theCheckHasPassed() {
        assertThat(world.result.passed()).withFailMessage("The check failed: %s", world.result.reason()).isTrue();
    }

    @Then("the check has failed")
    public void theCheckHasFailed() {
        assertThat(world.result.passed()).isFalse();
    }

    @Then("the soft check {string} has passed")
    public void theSoftCheckHasPassed(String name) {
        selectSoftCheck(name);
        theCheckHasPassed();
    }

    @Then("the soft check {string} has failed")
    public void theSoftCheckHasFailed(String name) {
        selectSoftCheck(name);
        theCheckHasFailed();
    }

    /** Takes the result of one check from the report, so the next steps read that one. */
    private void selectSoftCheck(String name) {
        world.result = world.report.results().stream()
            .filter(result -> result.checkName().equalsIgnoreCase(name))
            .findFirst()
            .orElseThrow(() -> new AssertionError("The report has no check \"" + name + "\""));
        assertThat(world.result.severity()).isEqualTo(CheckSeverity.SOFT);
    }

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
