package be.kdg.airfreight.loadplanning.acceptance.integration;

import java.util.UUID;
import java.util.function.Supplier;

import be.kdg.airfreight.loadplanning.TestClock;
import be.kdg.airfreight.loadplanning.TestHelper;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargoId;
import be.kdg.airfreight.loadplanning.domain.flightcargo.Uld;
import be.kdg.airfreight.loadplanning.domain.flightcargo.WeightUnit;
import be.kdg.airfreight.loadplanning.domain.loadplanner.LoadPlannerId;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static be.kdg.airfreight.loadplanning.acceptance.integration.Logins.namedToken;
import static be.kdg.airfreight.loadplanning.acceptance.integration.Logins.token;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Stories 33, 35 and 44 through the REST API of Load Planning: who is logged in, what they ask and what the API
 * answers. The cargo is that of HJ221 of 9 January 2026 with one booked ULD, at 05:00 UTC, inside its loading
 * period. Cucumber makes a new instance for every scenario, so the fields are the state of one scenario.
 */
public class PlannerAccessSteps {
    private static final String ULD = "AKE16689R7";

    private final MockMvc mockMvc;
    private final TestHelper testHelper;
    private final TestClock clock;

    private RequestPostProcessor login;
    private LoadPlannerId loggedInLoadPlanner;
    private FlightCargoId cargoId;
    private ResultActions answer;
    /** Who the action of the When step recorded: a load position records its planner, finalizing the load too. */
    private Supplier<LoadPlannerId> recorded;

    public PlannerAccessSteps(MockMvc mockMvc, TestHelper testHelper, TestClock clock) {
        this.mockMvc = mockMvc;
        this.testHelper = testHelper;
        this.clock = clock;
    }

    @After
    public void resetTheClock() {
        clock.reset();
    }

    /** A load planner is registered in Load Planning; a flight planner only has an account. */
    @Given("I am logged in as a {} planner")
    @Given("there is a user with the role {} planner in Keycloak")
    public void aPlannerIsLoggedIn(String role) {
        loggedInLoadPlanner = testHelper.saveLoadPlanner("Test load planner");
        UUID account = role.equals("load") ? loggedInLoadPlanner.id() : UUID.randomUUID();
        login = token(account, role + "-planner");
    }

    @Given("I am logged in as the load planner {word}")
    public void theLoadPlannerIsLoggedIn(String name) {
        loggedInLoadPlanner = testHelper.saveLoadPlanner(name);
        login = namedToken(loggedInLoadPlanner.id(), name, "load-planner");
    }

    /** Only Keycloak knows this planner: Load Planning registers them on their first change. */
    @Given("I am logged in as {word}, a load planner who is new in Keycloak")
    public void aNewLoadPlannerIsLoggedIn(String name) {
        loggedInLoadPlanner = new LoadPlannerId(UUID.randomUUID());
        login = namedToken(loggedInLoadPlanner.id(), name, "load-planner");
    }

    @Given("there is a user with neither of the two roles in Keycloak")
    public void aUserWithoutAPlannerRole() {
        login = token(UUID.randomUUID());
    }

    @Given("there is a user with both the flight planner and the load planner role in Keycloak")
    public void aUserWithBothPlannerRoles() {
        login = token(testHelper.saveLoadPlanner("Test load planner").id(), "flight-planner", "load-planner");
    }

    /** Logging in happens at Keycloak; here the user arrives with the token. The first request shows the role. */
    @When("that user logs in")
    public void thatUserLogsIn() throws Exception {
        requestTheLoad();
    }

    @When("I request the details and the load of HJ221")
    public void requestTheLoad() throws Exception {
        answer = mockMvc.perform(get("/api/flight-cargos/{id}", cargoOfHj221().id()).with(login));
    }

    @When("I assign a load position through the API")
    public void assignALoadPosition() throws Exception {
        answer = mockMvc.perform(assignToCl().with(login));
        recorded = () -> bookedUld().getPositionChangedBy();
    }

    @When("I check the load through the API")
    public void checkTheLoad() throws Exception {
        answer = mockMvc.perform(get("/api/flight-cargos/{id}/load-check", cargoOfHj221().id()).with(login));
    }

    /** The ULD has no load position: a soft check fails, which does not stop finalizing. */
    @When("I finalize the load through the API")
    public void finalizeTheLoad() throws Exception {
        answer = mockMvc.perform(post("/api/flight-cargos/{id}/finalize", cargoOfHj221().id()).with(login));
        recorded = () -> testHelper.find(cargoOfHj221()).getFinalizedBy();
    }

    @Then("that is accepted")
    public void thatIsAccepted() throws Exception {
        answer.andExpect(status().isOk());
    }

    @Then("that is refused with 403 Forbidden")
    @Then("they get no access and see why")
    public void thatIsForbidden() throws Exception {
        answer.andExpect(status().isForbidden());
    }

    @Then("I get to see them")
    public void iGetToSeeThem() throws Exception {
        answer.andExpect(status().isOk())
            .andExpect(jsonPath("$.flightNumber").value("HJ221"))
            .andExpect(jsonPath("$.ulds[0].code").value(ULD));
    }

    @Then("it is recorded that {word} did that")
    public void itIsRecordedWhoDidThat(String name) throws Exception {
        answer.andExpect(status().isOk());
        assertThat(recorded.get()).as("the load planner %s", name).isEqualTo(loggedInLoadPlanner);
    }

    @Then("the cargo shows that {word} put the ULD on its load position")
    public void theCargoShowsWhoPutTheUld(String name) throws Exception {
        answer.andExpect(status().isOk());
        requestTheLoad();
        answer.andExpect(status().isOk())
            .andExpect(jsonPath("$.ulds[0].loadPosition").value("CL"))
            .andExpect(jsonPath("$.ulds[0].positionChangedBy").value(name));
    }

    @Then("the cargo shows that {word} finalized the load")
    public void theCargoShowsWhoFinalized(String name) throws Exception {
        answer.andExpect(status().isOk());
        requestTheLoad();
        answer.andExpect(status().isOk())
            .andExpect(jsonPath("$.finalized").value(true))
            .andExpect(jsonPath("$.finalizedBy").value(name));
    }

    /** A load planner reads and changes the load; a flight planner reads it and is refused when changing it. */
    @Then("the system treats them as a {} planner")
    public void theSystemTreatsThemAs(String role) throws Exception {
        answer.andExpect(status().isOk());
        mockMvc.perform(assignToCl().with(login))
            .andExpect(role.equals("load") ? status().isOk() : status().isForbidden());
    }

    private MockHttpServletRequestBuilder assignToCl() {
        return put("/api/flight-cargos/{id}/ulds/{uldCode}/load-position", cargoOfHj221().id(), ULD)
            .contentType(MediaType.APPLICATION_JSON).content("{\"loadPosition\": \"CL\"}");
    }

    private FlightCargoId cargoOfHj221() {
        if (cargoId == null) {
            clock.set(TestHelper.IN_THE_LOADING_PERIOD);
            cargoId = testHelper.saveCargo().getId();
            testHelper.book(cargoId, ULD, "1250", WeightUnit.KG, "ATH");
        }
        return cargoId;
    }

    private Uld bookedUld() {
        return testHelper.find(cargoOfHj221()).getUlds().getFirst();
    }
}
