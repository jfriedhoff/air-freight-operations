package be.kdg.airfreight.simulator.acceptance.steps;

import be.kdg.airfreight.simulator.StreamProbe;
import com.jayway.jsonpath.JsonPath;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.core.env.Environment;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;

/** Steps that both stories share: unreadable JSON, the answer of the API, nothing being sent, and the profile. */
public class OutcomeSteps {
    private final SimulatorWorld world;
    private final StreamProbe probe;
    private final Environment environment;

    public OutcomeSteps(SimulatorWorld world, StreamProbe probe, Environment environment) {
        this.world = world;
        this.probe = probe;
        this.environment = environment;
    }

    @Before
    public void forgetEarlierMessages() {
        probe.clear();
    }

    /** Checks instead of sets: the profile is fixed when the context starts. Fails in the wrong suite. */
    @Given("the application runs with the production profile")
    public void theApplicationRunsWithTheProductionProfile() {
        assertThat(environment.getActiveProfiles()).contains("production");
    }

    /** The text does not say which records, so the broken JSON goes to every endpoint that exists. */
    @When("I supply JSON that cannot be read")
    public void supplyUnreadableJson() throws Exception {
        world.post(SimulatorWorld.FLIGHT_SCHEDULES, "[{ \"flightNumber\": ");
        world.post(SimulatorWorld.CARGO_BOOKINGS, "[{ \"flightNumber\": ");
    }

    @Then("the API responds with 400 Bad Request and the reason")
    public void respondsWithBadRequestAndTheReason() throws Exception {
        assertThat(world.responses()).isNotEmpty();
        for (MvcResult response : world.responses()) {
            assertThat(response.getResponse().getStatus()).isEqualTo(400);
            String detail = JsonPath.read(response.getResponse().getContentAsString(), "$.detail");
            assertThat(detail).startsWith("The JSON cannot be read: ").hasSizeGreaterThan(30);
        }
    }

    @Then("the API responds with 404 Not Found")
    public void respondsWithNotFound() {
        assertThat(world.responses()).isNotEmpty()
            .allSatisfy(response -> assertThat(response.getResponse().getStatus()).isEqualTo(404));
    }

    @Then("no SSM is sent")
    @Then("no ULD message is sent")
    public void nothingIsSent() {
        assertThat(probe.nothingSent()).isTrue();
    }
}
