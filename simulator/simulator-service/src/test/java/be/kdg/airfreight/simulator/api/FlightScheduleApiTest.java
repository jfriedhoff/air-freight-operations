package be.kdg.airfreight.simulator.api;

import java.util.List;

import be.kdg.airfreight.simulator.StreamProbe;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FlightScheduleApiTest {
    private static final String RECORD = """
        { "periodFrom": "2026-01-05", "periodTo": "2026-01-18", "weekdays": ["MONDAY"],
          "departureTime": "06:30", "arrivalTime": "09:45", "flightNumber": "%s",
          "departureAirport": "BRU", "arrivalAirport": "ATH", "registration": "SXBAA" }""";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StreamProbe probe;

    @BeforeEach
    void clear() {
        probe.clear();
    }

    @Test
    void twoRecordsAreAcceptedAndSentAsTwoSsmsWithTheReturnedIds() throws Exception {
        // Act
        String body = mockMvc.perform(post("/api/flight-schedules")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[" + RECORD.formatted("HJ221") + "," + RECORD.formatted("3V11") + "]"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.messageIds.length()").value(2))
            .andReturn().getResponse().getContentAsString();

        // Assert
        List<String> ids = JsonPath.read(body, "$.messageIds");
        assertThat(List.of(probe.nextSsm().messageId(), probe.nextSsm().messageId())).isEqualTo(ids);
    }

    @Test
    void aRecordWithAMissingFieldIsRefusedWithItsNameAndNothingIsSent() throws Exception {
        String withoutPeriodTo = RECORD.formatted("3V11").replace("\"periodTo\": \"2026-01-18\",", "");

        mockMvc.perform(post("/api/flight-schedules")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[" + RECORD.formatted("HJ221") + "," + withoutPeriodTo + "]"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value(containsString("periodTo is required")));

        assertThat(probe.nothingSent()).isTrue();
    }

    @Test
    void anEmptyListIsRefused() throws Exception {
        mockMvc.perform(post("/api/flight-schedules").contentType(MediaType.APPLICATION_JSON).content("[]"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value(containsString("at least one flight schedule record")));
    }

    @Test
    void aNullRecordIsRefused() throws Exception {
        mockMvc.perform(post("/api/flight-schedules").contentType(MediaType.APPLICATION_JSON).content("[null]"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value(containsString("A record cannot be null")));
    }

    @Test
    void aValueOfTheWrongTypeIsRefusedAsUnreadable() throws Exception {
        String badDate = RECORD.formatted("HJ221").replace("2026-01-05", "next monday");

        mockMvc.perform(post("/api/flight-schedules").contentType(MediaType.APPLICATION_JSON)
                .content("[" + badDate + "]"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value(containsString("The JSON cannot be read")));
    }

    /** By decision: Messaging judges the values, so an invalid message can be demonstrated. */
    @Test
    void aFlightNumberMessagingWillRefuseIsStillPassedOn() throws Exception {
        mockMvc.perform(post("/api/flight-schedules").contentType(MediaType.APPLICATION_JSON)
                .content("[" + RECORD.formatted("hj221") + "]"))
            .andExpect(status().isAccepted());

        assertThat(probe.nextSsm().body()).contains("\"flightNumber\":\"hj221\"");
    }
}
