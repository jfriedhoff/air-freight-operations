package be.kdg.airfreight.loadplanning.acceptance.integration;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import be.kdg.airfreight.loadplanning.TestClock;
import be.kdg.airfreight.loadplanning.TestHelper;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargoId;
import be.kdg.airfreight.loadplanning.domain.flightcargo.UldCode;
import be.kdg.airfreight.loadplanning.domain.flightcargo.WeightUnit;
import be.kdg.airfreight.loadplanning.domain.loadplanner.LoadPlannerId;
import com.jayway.jsonpath.JsonPath;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Story 18 through the REST API: two load planners who place a ULD on the same load position at the same moment.
 * The flight and its bookings are stored through TestHelper, because the story is not about messages.
 */
public class LoadPositionApiSteps {
    private final MockMvc mockMvc;
    private final TestHelper testHelper;
    private final TestClock clock;

    private FlightCargoId cargoId;
    private String position;
    private List<MockHttpServletResponse> answers;

    public LoadPositionApiSteps(MockMvc mockMvc, TestHelper testHelper, TestClock clock) {
        this.mockMvc = mockMvc;
        this.testHelper = testHelper;
        this.clock = clock;
    }

    @After
    public void resetTheClock() {
        clock.reset();
    }

    @Given("flight {word} with STD {moment} UTC")
    public void flightWithStd(String flightNumber, Instant std) {
        cargoId = testHelper.saveCargo(flightNumber, LocalDate.ofInstant(std, ZoneOffset.UTC),
            LocalTime.ofInstant(std, ZoneOffset.UTC)).getId();
    }

    @Given("{uld} is booked on {word}")
    @Given("{uld} is also booked on {word}")
    public void isBookedOn(UldCode code, String flightNumber) {
        assertThat(testHelper.find(cargoId).getFlightId().flightNumber().value()).isEqualTo(flightNumber);
        testHelper.book(cargoId, code.value(), "1250", WeightUnit.KG, "ATH");
    }

    @Given("it is {moment} UTC, within the loading period")
    public void itIs(Instant now) {
        clock.set(now);
    }

    /**
     * Both requests wait for one signal and then go at once. Whichever comes second is refused: when the two
     * overlap, the version of the cargo stops it; when one is done before the other starts, the occupied position.
     */
    @When("two load planners place {uld} and {uld} on {word} at the same time")
    public void twoLoadPlannersPlaceAtTheSameTime(UldCode first, UldCode second, String position) throws Exception {
        this.position = position;
        LoadPlannerId firstPlanner = testHelper.saveLoadPlanner("First load planner");
        LoadPlannerId secondPlanner = testHelper.saveLoadPlanner("Second load planner");
        CountDownLatch go = new CountDownLatch(1);
        try (ExecutorService twoPlanners = Executors.newFixedThreadPool(2)) {
            Future<MockHttpServletResponse> firstAnswer = twoPlanners.submit(() -> place(go, firstPlanner, first));
            Future<MockHttpServletResponse> secondAnswer = twoPlanners.submit(() -> place(go, secondPlanner, second));
            go.countDown();
            answers = List.of(firstAnswer.get(), secondAnswer.get());
        }
    }

    @Then("exactly one of the two ULDs is on {word}")
    public void exactlyOneIsOn(String position) {
        assertThat(answers).extracting(MockHttpServletResponse::getStatus).containsOnlyOnce(200);
        assertThat(testHelper.find(cargoId).getLoad())
            .filteredOn(uld -> uld.getPosition().name().equals(position))
            .hasSize(1);
    }

    @Then("the other load planner gets a refusal with an explanation")
    public void theOtherGetsARefusal() throws Exception {
        MockHttpServletResponse refused = answers.stream()
            .filter(answer -> answer.getStatus() != 200)
            .findFirst()
            .orElseThrow();
        assertThat(refused.getStatus()).isEqualTo(409);
        String detail = JsonPath.read(refused.getContentAsString(), "$.detail");
        assertThat(detail).isNotBlank();
    }

    private MockHttpServletResponse place(CountDownLatch go, LoadPlannerId planner, UldCode uld) throws Exception {
        go.await();
        return mockMvc.perform(put("/api/flight-cargos/{id}/ulds/{uldCode}/load-position", cargoId.id(), uld.value())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"loadPosition\": \"" + position + "\"}")
                .with(jwt().jwt(token -> token.subject(planner.id().toString()))
                    .authorities(new SimpleGrantedAuthority("ROLE_load-planner"))))
            .andReturn().getResponse();
    }
}
