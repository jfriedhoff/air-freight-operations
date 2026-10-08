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
class CargoBookingApiTest {
    private static final String RECORD = """
        { "flightNumber": "HJ221", "scheduledDepartureDate": "2026-01-09", "departureAirport": "BRU",
          "uldCode": "%s", "weight": 1250.5, "weightUnit": "KG", "destination": "ATH" }""";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StreamProbe probe;

    @BeforeEach
    void clear() {
        probe.clear();
    }

    @Test
    void threeRecordsAreAcceptedAndSentAsThreeUldMessagesWithTheReturnedIds() throws Exception {
        // Act
        String body = mockMvc.perform(post("/api/cargo-bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[" + RECORD.formatted("AKE16689R7") + "," + RECORD.formatted("PMC12345KL") + ","
                    + RECORD.formatted("AKE22222R7") + "]"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.messageIds.length()").value(3))
            .andReturn().getResponse().getContentAsString();

        // Assert
        List<String> ids = JsonPath.read(body, "$.messageIds");
        assertThat(List.of(probe.nextUldMessage().messageId(), probe.nextUldMessage().messageId(),
            probe.nextUldMessage().messageId())).isEqualTo(ids);
    }

    @Test
    void aRecordWithoutAScheduledDepartureDateIsRefusedWithItsNameAndNothingIsSent() throws Exception {
        String withoutDate = RECORD.formatted("PMC12345KL").replace("\"scheduledDepartureDate\": \"2026-01-09\",", "");

        mockMvc.perform(post("/api/cargo-bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[" + RECORD.formatted("AKE16689R7") + "," + withoutDate + "]"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value(containsString("scheduledDepartureDate is required")));

        assertThat(probe.nothingSent()).isTrue();
    }

    @Test
    void anEmptyListIsRefused() throws Exception {
        mockMvc.perform(post("/api/cargo-bookings").contentType(MediaType.APPLICATION_JSON).content("[]"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value(containsString("at least one booking record")));
    }
}
