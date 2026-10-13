package be.kdg.airfreight.flightoperations.acceptance.integration;

import be.kdg.airfreight.flightoperations.TestHelper;
import be.kdg.airfreight.flightoperations.domain.flight.Flight;
import be.kdg.airfreight.flightoperations.domain.flight.FlightId;
import ch.qos.logback.classic.Level;
import io.cucumber.java.After;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Stories #5 and #8 through the REST API: who is logged in, what they send and what the API answers.
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

    @Given("I am logged in as {}")
    public void iAmLoggedInAs(String who) {
        login = switch (who) {
            case "a flight planner" -> token(testHelper.saveFlightPlanner().id(), "flight-planner");
            case "a flight planner who is not registered" -> token(UUID.randomUUID(), "flight-planner");
            case "a load planner" -> token(UUID.randomUUID(), "load-planner");
            case "a user with both planner roles" ->
                token(testHelper.saveFlightPlanner().id(), "flight-planner", "load-planner");
            case "nobody" -> null;
            default -> throw new IllegalArgumentException("Unknown user: " + who);
        };
    }

    @When("I enter a departure delay of {int} minutes through the API")
    public void iEnterADepartureDelay(int minutes) throws Exception {
        enterDelay(flight().id(), minutes, "93");
    }

    @When("I enter a departure delay with delay reason {word} through the API")
    public void iEnterADepartureDelayWithReason(String reason) throws Exception {
        enterDelay(flight().id(), 25, reason);
    }

    @When("I enter a departure delay for a flight that does not exist through the API")
    public void iEnterADepartureDelayForAnUnknownFlight() throws Exception {
        enterDelay(UUID.randomUUID(), 25, "93");
    }

    /** The API names a flight by its id: a flight that does not exist has an id that nobody knows. */
    @When("I enter a delay through the API for {word} of {date} from {word}, a flight that does not exist")
    public void iEnterADelayForAFlightThatDoesNotExist(String flightNumber, LocalDate date, String departureAirport)
        throws Exception {
        assertThat(testHelper.findFlight(flightNumber, date, departureAirport)).isEmpty();
        enterDelay(UUID.randomUUID(), 25, "93");
    }

    @When("the flight planner enters a departure delay of {int} minutes")
    public void theFlightPlannerEntersADepartureDelay(int minutes) throws Exception {
        iAmLoggedInAs("a flight planner");
        iEnterADepartureDelay(minutes);
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

    @And("HJ221 has no delay")
    public void hj221HasNoDelay() {
        assertThat(testHelper.find(flight()).getDelays()).isEmpty();
        assertThat(testHelper.outboxTypes()).isEmpty();
    }

    @And("the explanation says {string}")
    public void theExplanationSays(String explanation) throws Exception {
        lastAnswer().andExpect(jsonPath("$.detail").value(explanation));
    }

    private void enterDelay(UUID flight, int minutes, String reason) throws Exception {
        send(post("/api/flights/{id}/delays", flight).contentType(MediaType.APPLICATION_JSON).content("""
            {"kind": "DEPARTURE", "minutes": %d, "reason": "%s", "description": "Late inbound aircraft"}"""
            .formatted(minutes, reason)));
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
