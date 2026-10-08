package be.kdg.airfreight.simulator.acceptance.steps;

import java.util.ArrayList;
import java.util.List;

import io.cucumber.spring.ScenarioScope;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/** The state of one scenario: what "I" supplied and what the API answered. A new one per scenario. */
@Component
@ScenarioScope
public class SimulatorWorld {
    public static final String FLIGHT_SCHEDULES = "/api/flight-schedules";
    public static final String CARGO_BOOKINGS = "/api/cargo-bookings";

    private final MockMvc mockMvc;
    private final List<MvcResult> responses = new ArrayList<>();

    SimulatorWorld(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    public void post(String url, String json) throws Exception {
        responses.add(mockMvc.perform(MockMvcRequestBuilders.post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andReturn());
    }

    public List<MvcResult> responses() {
        return responses;
    }

    public String lastBody() throws Exception {
        return responses.getLast().getResponse().getContentAsString();
    }
}
