package be.kdg.airfreight.flightoperations.acceptance.integration;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import be.kdg.airfreight.flightoperations.TestHelper;
import be.kdg.airfreight.flightoperations.domain.flight.Delay;
import be.kdg.airfreight.flightoperations.domain.flight.DelayKind;
import be.kdg.airfreight.flightoperations.domain.flight.Flight;
import be.kdg.airfreight.flightoperations.domain.flight.FlightId;
import be.kdg.airfreight.flightoperations.domain.flightplanner.FlightPlannerId;
import ch.qos.logback.classic.Level;
import io.cucumber.java.After;
import io.cucumber.java.en.And;
import io.cucumber.java.en.But;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Stories #5, #8, #9, #33 and #35 through the REST API: who is logged in, what they send and what the API answers.
 * Cucumber makes a new instance for every scenario, so the fields are the state of one scenario.
 */
public class FlightApiSteps {
    private static final String CHANGED_TIMES = """
        {"std": "2026-01-09T08:00:00Z", "etd": "2026-01-09T08:00:00Z"}""";

    private final MockMvc mockMvc;
    private final TestHelper testHelper;
    private final LogCapture logs;

    /** Who sends the request; null is nobody: a request without a token. */
    private RequestPostProcessor login;
    private FlightId flightId;
    private final List<ResultActions> answers = new ArrayList<>();
    private final Map<String, FlightPlannerId> plannersByName = new HashMap<>();

    public FlightApiSteps(MockMvc mockMvc, TestHelper testHelper, LogCapture logs) {
        this.mockMvc = mockMvc;
        this.testHelper = testHelper;
        this.logs = logs;
    }

    @After
    public void cleanUp() {
        testHelper.cleanUp();
    }

    /** The schedule in the step is the one TestHelper plans: 9 January 2026, STD 06:30 and STA 09:45 UTC. */
    @Given("^flight HJ221 of 9 January 2026 is planned with STD 06:30 and STA 09:45 UTC$")
    @Given("flight HJ221 of 9 January 2026 with STD 06:30 and STA 09:45 UTC")
    public void flightHj221IsPlanned() {
        flightId = testHelper.savePlannedFlight().getId();
    }

    @Given("flight HJ221 has departed")
    public void flightHj221HasDeparted() {
        testHelper.depart(flight());
    }

    @Given("flight HJ221 has landed")
    public void flightHj221HasLanded() {
        testHelper.depart(flight());
        testHelper.land(flight());
    }

    @Given("I am logged in as a flight planner")
    @Given("there is a user with the role flight planner in Keycloak")
    public void aFlightPlannerIsLoggedIn() {
        login = token(testHelper.saveFlightPlanner().id(), "flight-planner");
    }

    @Given("I am logged in as a flight planner who is not registered")
    public void aFlightPlannerWhoIsNotRegisteredIsLoggedIn() {
        login = token(UUID.randomUUID(), "flight-planner");
    }

    @Given("I am logged in as a load planner")
    @Given("there is a user with the role load planner in Keycloak")
    public void aLoadPlannerIsLoggedIn() {
        login = token(UUID.randomUUID(), "load-planner");
    }

    @Given("I am logged in as a user with both planner roles")
    @Given("there is a user with both the flight planner and the load planner role in Keycloak")
    public void aUserWithBothPlannerRolesIsLoggedIn() {
        login = token(testHelper.saveFlightPlanner().id(), "flight-planner", "load-planner");
    }

    @Given("there is a user with neither of the two roles in Keycloak")
    public void aUserWithoutAPlannerRoleIsLoggedIn() {
        login = token(UUID.randomUUID());
    }

    @Given("I am logged in as nobody")
    @Given("I am not logged in")
    public void nobodyIsLoggedIn() {
        login = null;
    }

    /** A registered flight planner with a name, so a step can say who made a change. */
    @Given("I am logged in as the flight planner {word}")
    public void theFlightPlannerIsLoggedIn(String name) {
        FlightPlannerId planner = testHelper.saveFlightPlanner(name);
        plannersByName.put(name, planner);
        login = token(planner.id(), "flight-planner");
    }

    @When("I enter a(n) {delayKind} delay of {int} minutes through the API")
    public void iEnterADelay(DelayKind kind, int minutes) throws Exception {
        enterDelay(flight().id(), kind, minutes, "93");
    }

    @When("I enter a departure delay with delay reason {word} through the API")
    public void iEnterADepartureDelayWithReason(String reason) throws Exception {
        enterDelay(flight().id(), DelayKind.DEPARTURE, 25, reason);
    }

    @When("I enter a departure delay for a flight that does not exist through the API")
    public void iEnterADepartureDelayForAnUnknownFlight() throws Exception {
        enterDelay(UUID.randomUUID(), DelayKind.DEPARTURE, 25, "93");
    }

    /** The API names a flight by its id: a flight that does not exist has an id that nobody knows. */
    @When("I enter a delay through the API for {word} of {date} from {word}, a flight that does not exist")
    public void iEnterADelayForAFlightThatDoesNotExist(String flightNumber, LocalDate date, String departureAirport)
        throws Exception {
        assertThat(testHelper.findFlight(flightNumber, date, departureAirport)).isEmpty();
        enterDelay(UUID.randomUUID(), DelayKind.DEPARTURE, 25, "93");
    }

    @When("the flight planner enters a departure delay of {int} minutes")
    public void theFlightPlannerEntersADepartureDelay(int minutes) throws Exception {
        aFlightPlannerIsLoggedIn();
        iEnterADelay(DelayKind.DEPARTURE, minutes);
    }

    /** Story #5 has no Background: the flight is planned here. Both ways to change a resource are tried. */
    @When("I try to change the STD or the ETD of HJ221 through the API")
    public void iTryToChangeTheStdOrTheEtd() throws Exception {
        flightId = testHelper.savePlannedFlight().getId();
        send(put("/api/flights/{id}", flight().id()).contentType(MediaType.APPLICATION_JSON).content(CHANGED_TIMES));
        send(patch("/api/flights/{id}", flight().id()).contentType(MediaType.APPLICATION_JSON)
            .content(CHANGED_TIMES));
    }

    @Then("the API answers {int}")
    @Then("the API responds with {int} Not Found")
    public void theApiAnswers(int statusCode) throws Exception {
        lastAnswer().andExpect(status().is(statusCode));
    }

    @Then("that is refused")
    public void thatIsRefused() throws Exception {
        assertThat(answers).hasSize(2);
        for (ResultActions answer : answers) {
            answer.andExpect(status().isMethodNotAllowed());
        }
    }

    @Then("he gets a confirmation")
    public void heGetsAConfirmation() throws Exception {
        lastAnswer().andExpect(status().isOk());
    }

    @And("the delay has been stored")
    public void theDelayHasBeenStored() {
        assertThat(testHelper.find(flight()).getDelays()).hasSize(1);
    }

    @And("the event is waiting in the outbox of Flight Operations")
    public void theEventIsWaitingInTheOutbox() {
        assertThat(testHelper.unsentOutboxTypes()).containsExactly(
            "flight-operations.flight.departure-estimate-changed",
            "flight-operations.flight.arrival-estimate-changed");
    }

    @And("the attempt is logged with a warning")
    public void theAttemptIsLoggedWithAWarning() {
        logs.assertLogged(Level.WARN, "does not exist");
    }

    @And("the STD and the ETD remain unchanged")
    public void theStdAndTheEtdRemainUnchanged() {
        Flight flight = testHelper.find(flight());
        assertThat(flight.getScheduled().departure()).isEqualTo(TestHelper.STD);
        assertThat(flight.getEstimated().departure()).isEqualTo(TestHelper.STD);
    }

    @And("the API shows the ETD {word} and the ETA {word}")
    public void theApiShowsTheEtdAndTheEta(String etd, String eta) throws Exception {
        lastAnswer()
            .andExpect(jsonPath("$.etd").value("2026-01-09T" + etd + ":00Z"))
            .andExpect(jsonPath("$.eta").value("2026-01-09T" + eta + ":00Z"));
    }

    @And("DepartureEstimateChanged and ArrivalEstimateChanged wait in the outbox")
    public void bothEstimateEventsWaitInTheOutbox() {
        assertThat(testHelper.outboxTypes()).containsExactly(
            "flight-operations.flight.departure-estimate-changed",
            "flight-operations.flight.arrival-estimate-changed");
    }

    @And("only ArrivalEstimateChanged waits in the outbox")
    public void onlyTheArrivalEstimateEventWaitsInTheOutbox() {
        assertThat(testHelper.outboxTypes()).containsExactly("flight-operations.flight.arrival-estimate-changed");
    }

    @And("HJ221 has no delay")
    public void hj221HasNoDelay() {
        assertThat(testHelper.find(flight()).getDelays()).isEmpty();
        assertThat(testHelper.outboxTypes()).isEmpty();
    }

    @And("the explanation says {string}")
    public void theExplanationSays(String explanation) throws Exception {
        lastAnswer().andExpect(jsonPath("$.detail").value(explanation));
    }

    /** Stories 33 and 35 name no flight: HJ221 of TestHelper is planned when the scenario has none yet. */
    @When("I enter a delay through the API")
    @When("I try to enter a delay through the API")
    public void iEnterADelay() throws Exception {
        enterDelay(hj221().id(), DelayKind.DEPARTURE, 25, "93");
    }

    /** A field that names a planner is not part of the request: the planner is the one of the token. */
    @When("I enter a delay through the API with the planner {word} in the request")
    public void iEnterADelayNamingAnotherPlanner(String name) throws Exception {
        FlightPlannerId other = testHelper.saveFlightPlanner(name);
        send(post("/api/flights/{id}/delays", hj221().id()).contentType(MediaType.APPLICATION_JSON).content("""
            {"kind": "DEPARTURE", "minutes": 25, "reason": "93", "description": "Late inbound aircraft",
             "planner": "%s", "by": "%s"}""".formatted(other.id(), other.id())));
    }

    /** Logging in happens at Keycloak; here the user arrives with the token. The first request shows the role. */
    @When("that user logs in")
    @When("I request the details and the load of HJ221")
    public void iRequestTheDetailsOfHj221() throws Exception {
        send(get("/api/flights/{id}", hj221().id()));
    }

    @Then("that is accepted")
    public void thatIsAccepted() throws Exception {
        lastAnswer().andExpect(status().isOk());
    }

    @Then("that is refused with 403 Forbidden")
    @Then("they get no access and see why")
    public void thatIsForbidden() throws Exception {
        lastAnswer().andExpect(status().isForbidden());
    }

    @Then("the API responds with 401 Unauthorized")
    public void theApiRespondsUnauthorized() throws Exception {
        lastAnswer().andExpect(status().isUnauthorized());
    }

    @But("not with a redirect to the login page")
    public void notWithARedirect() throws Exception {
        lastAnswer().andExpect(header().doesNotExist("Location"));
    }

    @Then("I get to see them")
    public void iGetToSeeThem() throws Exception {
        lastAnswer().andExpect(status().isOk()).andExpect(jsonPath("$.flightNumber").value("HJ221"));
    }

    @Then("it is recorded that {word} did that")
    @Then("it is recorded that {word} entered the delay")
    public void itIsRecordedWhoEnteredTheDelay(String name) throws Exception {
        lastAnswer().andExpect(status().isOk());
        assertThat(testHelper.find(flight()).getDelays()).singleElement()
            .extracting(Delay::getLastChangedBy).as("the flight planner %s", name).isEqualTo(plannersByName.get(name));
    }

    /** A flight planner reads and enters a delay; a load planner reads and is refused when entering one. */
    @Then("the system treats them as a {word} planner")
    public void theSystemTreatsThemAs(String role) throws Exception {
        lastAnswer().andExpect(status().isOk());
        iEnterADelay();
        lastAnswer().andExpect(role.equals("flight") ? status().isOk() : status().isForbidden());
    }

    private FlightId hj221() {
        if (flightId == null) {
            flightId = testHelper.savePlannedFlight().getId();
        }
        return flightId;
    }

    private void enterDelay(UUID flight, DelayKind kind, int minutes, String reason) throws Exception {
        send(post("/api/flights/{id}/delays", flight).contentType(MediaType.APPLICATION_JSON).content("""
            {"kind": "%s", "minutes": %d, "reason": "%s", "description": "Late inbound aircraft"}"""
            .formatted(kind, minutes, reason)));
    }

    private void send(MockHttpServletRequestBuilder request) throws Exception {
        answers.add(mockMvc.perform(login == null ? request : request.with(login)));
    }

    private ResultActions lastAnswer() {
        return answers.getLast();
    }

    private FlightId flight() {
        assertThat(flightId).as("the flight of the scenario").isNotNull();
        return flightId;
    }

    private static RequestPostProcessor token(UUID subject, String... roles) {
        SimpleGrantedAuthority[] authorities = Arrays.stream(roles)
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
            .toArray(SimpleGrantedAuthority[]::new);
        return jwt().jwt(token -> token.subject(subject.toString())).authorities(authorities);
    }
}
