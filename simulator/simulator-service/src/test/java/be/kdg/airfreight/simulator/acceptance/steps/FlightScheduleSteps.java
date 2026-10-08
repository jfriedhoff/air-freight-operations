package be.kdg.airfreight.simulator.acceptance.steps;

import java.time.Instant;

import be.kdg.airfreight.simulator.SentMessage;
import be.kdg.airfreight.simulator.StreamProbe;
import com.jayway.jsonpath.JsonPath;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.skyscreamer.jsonassert.JSONAssert;

import static org.assertj.core.api.Assertions.assertThat;

/** Story 31: flight schedule records in, SSMs out. */
public class FlightScheduleSteps {
    static final String HJ221_ON_MONDAYS = """
        { "periodFrom": "2026-01-05", "periodTo": "2026-01-18", "weekdays": ["MONDAY"],
          "departureTime": "06:30", "arrivalTime": "09:45", "flightNumber": "HJ221",
          "departureAirport": "BRU", "arrivalAirport": "ATH", "registration": "SXBAA" }""";

    private final SimulatorWorld world;
    private final StreamProbe probe;

    public FlightScheduleSteps(SimulatorWorld world, StreamProbe probe) {
        this.world = world;
        this.probe = probe;
    }

    @When("I supply a valid flight schedule record")
    public void supplyAValidRecord() throws Exception {
        world.post(SimulatorWorld.FLIGHT_SCHEDULES, "[" + HJ221_ON_MONDAYS + "]");
    }

    @When("I supply flight schedule records")
    public void supplyFlightScheduleRecords() throws Exception {
        world.post(SimulatorWorld.FLIGHT_SCHEDULES, "[" + HJ221_ON_MONDAYS + "," + HJ221_ON_MONDAYS + "]");
    }

    /** Strict comparison: a missing field fails, and so does a field the contract does not have. */
    @Then("the sent SSM conforms to the fixed structure of an SSM")
    public void theSentSsmConformsToTheFixedStructure() throws Exception {
        SentMessage ssm = probe.nextSsm();
        String messageId = JsonPath.read(world.lastBody(), "$.messageIds[0]");
        String createdAt = JsonPath.read(ssm.body(), "$.createdAt");

        assertThat(ssm.routingKey()).isEqualTo("network-planning.ssm");
        assertThat(ssm.messageId()).isEqualTo(messageId);
        assertThat(Instant.parse(createdAt)).isBeforeOrEqualTo(Instant.now());
        JSONAssert.assertEquals("""
            { "messageId": "%s", "type": "network-planning.ssm", "version": 1, "createdAt": "%s",
              "periodFrom": "2026-01-05", "periodTo": "2026-01-18", "weekdays": ["MONDAY"],
              "departureTime": "06:30", "arrivalTime": "09:45", "flightNumber": "HJ221",
              "departureAirport": "BRU", "arrivalAirport": "ATH", "registration": "SXBAA" }
            """.formatted(messageId, createdAt), ssm.body(), true);
    }
}
