package be.kdg.airfreight.simulator.acceptance.steps;

import be.kdg.airfreight.simulator.StreamProbe;
import com.jayway.jsonpath.JsonPath;
import io.cucumber.java.Before;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;

/** Steps that both stories share: unreadable JSON, the answer of the API, and nothing being sent. */
public class OutcomeSteps {
    private final SimulatorWorld world;
    private final StreamProbe probe;

    public OutcomeSteps(SimulatorWorld world, StreamProbe probe) {
        this.world = world;
        this.probe = probe;
    }

    @Before
    public void forgetEarlierMessages() {
        probe.clear();
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

    @Then("no SSM is sent")
    @Then("no ULD message is sent")
    public void nothingIsSent() {
        assertThat(probe.nothingSent()).isTrue();
    }
}
