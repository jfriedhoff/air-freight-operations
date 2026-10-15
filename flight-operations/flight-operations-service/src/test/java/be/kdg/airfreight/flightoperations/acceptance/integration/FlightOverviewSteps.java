package be.kdg.airfreight.flightoperations.acceptance.integration;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

import be.kdg.airfreight.flightoperations.TestClock;
import be.kdg.airfreight.flightoperations.TestHelper;
import io.cucumber.java.After;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Story #4 through the API that the overview asks: the flights of one UTC day, today by default. The stories are
 * in 2026; every operations employee may read, so the requests are sent by a load planner.
 */
public class FlightOverviewSteps {
    private static final int YEAR = 2026;
    private static final LocalDate A_DAY_WITHOUT_FLIGHTS = LocalDate.of(YEAR, 2, 1);
    private static final ZoneId BRUSSELS = ZoneId.of("Europe/Brussels");

    private final MockMvc mockMvc;
    private final TestHelper testHelper;
    private final TestClock clock;

    private ResultActions answer;

    public FlightOverviewSteps(MockMvc mockMvc, TestHelper testHelper, TestClock clock) {
        this.mockMvc = mockMvc;
        this.testHelper = testHelper;
        this.clock = clock;
    }

    @After
    public void cleanUp() {
        testHelper.cleanUp();
        clock.reset();
    }

    @Given("it is {moment} UTC and there are flights on 8 and 9 January")
    public void itIsAndThereAreFlightsOn8And9January(Instant now) {
        clock.set(now);
        flightsOn8And9January();
    }

    @Given("I am viewing the overview of {int} {month}")
    public void iAmViewingTheOverviewOf(int day, Month month) throws Exception {
        flightsOn8And9January();
        iOpenTheOverviewOf(day, month);
    }

    @Given("flight {word} has STD {moment} UTC, in Brussels {int} {month} {time}")
    public void flightHasStd(String flightNumber, Instant std, int brusselsDay, Month brusselsMonth,
                             LocalTime brusselsTime) {
        ZonedDateTime inBrussels = std.atZone(BRUSSELS);
        assertThat(inBrussels.toLocalDate()).as("the date in Brussels in the scenario")
            .isEqualTo(LocalDate.of(YEAR, brusselsMonth, brusselsDay));
        assertThat(inBrussels.toLocalTime()).as("the time in Brussels in the scenario").isEqualTo(brusselsTime);
        testHelper.saveFlight(flightNumber, "BRU", LocalDate.ofInstant(std, ZoneOffset.UTC),
            LocalTime.ofInstant(std, ZoneOffset.UTC), "SXBAA");
    }

    /** The page sends no day: the server decides what today is. */
    @When("I open the flight overview")
    public void iOpenTheFlightOverview() throws Exception {
        answer = mockMvc.perform(asAnOperationsEmployee(get("/api/flights")));
    }

    @When("I choose {int} {month}")
    @When("I open the overview of {int} {month}")
    public void iOpenTheOverviewOf(int day, Month month) throws Exception {
        ask(LocalDate.of(YEAR, month, day));
    }

    @When("I choose a day on which there are no flights")
    public void iChooseADayWithoutFlights() throws Exception {
        ask(A_DAY_WITHOUT_FLIGHTS);
    }

    @Then("I only see the flights of {int} {month}")
    @Then("I see the flights of {int} {month}")
    public void iSeeTheFlightsOf(int day, Month month) throws Exception {
        String date = LocalDate.of(YEAR, month, day).toString();
        answer.andExpect(status().isOk())
            .andExpect(jsonPath("$.scheduledDepartureDate").value(date))
            .andExpect(jsonPath("$.flights").value(not(empty())))
            .andExpect(jsonPath("$.flights[*].scheduledDepartureDate").value(everyItem(is(date))));
    }

    @And("for each flight the flight number, the scheduled departure date and the departure and arrival airport")
    public void forEachFlightItsIdentityAndAirports() throws Exception {
        answer.andExpect(jsonPath("$.flights.length()").value(2))
            .andExpect(jsonPath("$.flights[0].flightNumber").value("HJ221"))
            .andExpect(jsonPath("$.flights[0].departureAirport").value("BRU"))
            .andExpect(jsonPath("$.flights[0].arrivalAirport").value("ATH"))
            .andExpect(jsonPath("$.flights[1].flightNumber").value("3V11"))
            .andExpect(jsonPath("$.flights[1].departureAirport").value("BRU"))
            .andExpect(jsonPath("$.flights[1].arrivalAirport").value("ATH"));
    }

    @Then("I see that there are no flights that day")
    public void iSeeThatThereAreNoFlights() throws Exception {
        answer.andExpect(status().isOk())
            .andExpect(jsonPath("$.scheduledDepartureDate").value(A_DAY_WITHOUT_FLIGHTS.toString()))
            .andExpect(jsonPath("$.flights.length()").value(0));
    }

    @Then("I see {word} with scheduled departure date {date}")
    public void iSeeWithScheduledDepartureDate(String flightNumber, LocalDate date) throws Exception {
        answer.andExpect(status().isOk())
            .andExpect(jsonPath("$.flights.length()").value(1))
            .andExpect(jsonPath("$.flights[0].flightNumber").value(flightNumber))
            .andExpect(jsonPath("$.flights[0].scheduledDepartureDate").value(date.toString()));
    }

    /**
     * What this step checks is the API part only: the answer names the UTC day it lists. The sentence on the page
     * that says so is fixed text; the @ui tag keeps it in the manual check.
     */
    @And("the screen states that the day is the scheduled departure date in UTC")
    public void theAnswerNamesTheUtcDayItLists() throws Exception {
        answer.andExpect(jsonPath("$.scheduledDepartureDate").value("2026-01-09"));
    }

    /** HJ221 on 8 January; HJ221 (06:30) and 3V11 (12:00) on 9 January, all from BRU to ATH. */
    private void flightsOn8And9January() {
        testHelper.saveFlight("HJ221", "BRU", LocalDate.of(YEAR, 1, 8), LocalTime.of(6, 30), "SXBAA");
        testHelper.saveFlight("HJ221", "BRU", LocalDate.of(YEAR, 1, 9), LocalTime.of(6, 30), "SXBAA");
        testHelper.saveFlight("3V11", "BRU", LocalDate.of(YEAR, 1, 9), LocalTime.of(12, 0), "SXBAB");
    }

    private void ask(LocalDate day) throws Exception {
        answer = mockMvc.perform(asAnOperationsEmployee(get("/api/flights")
            .param("scheduledDepartureDate", day.toString())));
    }

    private static MockHttpServletRequestBuilder asAnOperationsEmployee(MockHttpServletRequestBuilder request) {
        return request.with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()))
            .authorities(new SimpleGrantedAuthority("ROLE_load-planner")));
    }
}
