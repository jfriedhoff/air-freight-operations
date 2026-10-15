package be.kdg.airfreight.loadplanning.acceptance.integration;

import java.util.List;
import java.util.Map;

import be.kdg.airfreight.loadplanning.TestClock;
import be.kdg.airfreight.loadplanning.TestHelper;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargoId;
import be.kdg.airfreight.loadplanning.domain.flightcargo.WeightUnit;
import com.jayway.jsonpath.JsonPath;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static be.kdg.airfreight.loadplanning.acceptance.integration.Logins.token;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Story 24 through the REST API: Check Load and Finalize Load on the cargo of HJ221 of 9 January 2026, at 05:00
 * UTC inside its loading period. The unit scenarios of story 24 check the rules on the aggregate; these check
 * what only the API gives: the report in the answer, and the checks running again when the load changed in
 * between. Cucumber makes a new instance for every scenario, so the fields are the state of one scenario.
 */
public class FinalizeLoadSteps {
    private static final String CARGO = "/api/flight-cargos/{id}";
    private static final String LATERAL_BALANCE = "$.results[?(@.checkName == 'Lateral balance')]";

    private final MockMvc mockMvc;
    private final TestHelper testHelper;
    private final TestClock clock;

    private FlightCargoId cargoId;
    private MvcResult finalizeAnswer;

    public FinalizeLoadSteps(MockMvc mockMvc, TestHelper testHelper, TestClock clock) {
        this.mockMvc = mockMvc;
        this.testHelper = testHelper;
        this.clock = clock;
    }

    @After
    public void resetTheClock() {
        clock.reset();
    }

    @Given("it is within the loading period")
    public void itIsWithinTheLoadingPeriod() {
        clock.set(TestHelper.IN_THE_LOADING_PERIOD);
        cargoId = testHelper.saveCargo().getId();
    }

    /** 1000 kg left and 1200 kg right: the heaviest side may carry at most 1100 kg. */
    @Given("the lateral balance fails")
    public void theLateralBalanceFails() {
        bookAndPlace("AKE16689R7", "1000", "CL");
        bookAndPlace("AKE20417HJ", "1200", "CR");
    }

    @Given("Check Load showed that all hard checks pass")
    public void checkLoadShowedThatAllHardChecksPass() throws Exception {
        itIsWithinTheLoadingPeriod();
        bookAndPlace("AKE16689R7", "1000", "CL");
        bookAndPlace("AKE20417HJ", "1000", "CR");
        assertThat(JsonPath.<Boolean>read(checkLoad(), "$.hardChecksPassed")).isTrue();
    }

    /** The ULD on CL goes to DR, on the right: 0 kg left and 2000 kg right. */
    @Given("another load planner then moved a ULD, so the lateral balance fails")
    public void anotherLoadPlannerMovedAUld() throws Exception {
        mockMvc.perform(put(CARGO + "/ulds/{uldCode}/load-position", cargoId.id(), "AKE16689R7")
                .contentType(MediaType.APPLICATION_JSON).content("{\"loadPosition\": \"DR\"}")
                .with(aLoadPlanner("Another load planner")))
            .andExpect(status().isOk());
    }

    @When("I click Finalize Load")
    public void iClickFinalizeLoad() throws Exception {
        finalizeAnswer = mockMvc.perform(post(CARGO + "/finalize", cargoId.id())
            .with(aLoadPlanner("Test load planner"))).andReturn();
    }

    /** Same checks, same order, same results and reasons: the report of the refusal and that of Check Load. */
    @Then("I see the same report as with Check Load")
    public void iSeeTheSameReportAsWithCheckLoad() throws Exception {
        Map<String, Object> report = refusalReport();
        assertThat(lateralBalance(report)).as("the report has the failed check").containsExactly(false);
        assertThat(report).isEqualTo(JsonPath.read(checkLoad(), "$"));
    }

    @Then("the load is not finalized")
    public void theLoadIsNotFinalized() {
        assertThat(testHelper.find(cargoId).isFinalized()).isFalse();
    }

    @Then("I see the report with the failed lateral balance")
    public void iSeeTheReportWithTheFailedLateralBalance() throws Exception {
        Map<String, Object> report = refusalReport();
        assertThat(lateralBalance(report)).containsExactly(false);
        assertThat(JsonPath.<List<String>>read(report, LATERAL_BALANCE + ".reason")).containsExactly(
            "Left carries 0 kg and right 2000 kg; the heaviest side may carry at most 0 kg");
    }

    /** Finalize Load refuses with 409 and carries the report in the Problem Details. */
    private Map<String, Object> refusalReport() throws Exception {
        assertThat(finalizeAnswer.getResponse().getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        String body = finalizeAnswer.getResponse().getContentAsString();
        assertThat(JsonPath.<String>read(body, "$.code")).isEqualTo("hard-check-failed");
        return JsonPath.read(body, "$.report");
    }

    private String checkLoad() throws Exception {
        return mockMvc.perform(get(CARGO + "/load-check", cargoId.id()).with(aLoadPlanner("Test load planner")))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
    }

    private static List<Boolean> lateralBalance(Map<String, Object> report) {
        return JsonPath.read(report, LATERAL_BALANCE + ".passed");
    }

    private void bookAndPlace(String uldCode, String kilograms, String position) {
        testHelper.book(cargoId, uldCode, kilograms, WeightUnit.KG, "ATH");
        testHelper.place(cargoId, uldCode, position);
    }

    private RequestPostProcessor aLoadPlanner(String name) {
        return token(testHelper.saveLoadPlanner(name).id(), "load-planner");
    }
}
