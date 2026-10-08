package be.kdg.airfreight.simulator;

import java.net.ConnectException;

import be.kdg.airfreight.simulator.infrastructure.messaging.SsmMessage;
import be.kdg.airfreight.simulator.infrastructure.messaging.StreamPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Outside the api package: it replaces a bean of the infrastructure layer, which api itself may not touch. */
@SpringBootTest
@AutoConfigureMockMvc
class StreamUnavailableApiTest {
    @Autowired
    private MockMvc mockMvc;

    /** Replaces the real publisher: a real broker cannot be switched off from a test. */
    @MockitoBean
    private StreamPublisher publisher;

    @Test
    void whenRabbitMqCannotBeReachedTheApiSaysSoInsteadOfFailing() throws Exception {
        // Arrange
        willThrow(new AmqpConnectException(new ConnectException("Connection refused")))
            .given(publisher).send(any(SsmMessage.class));

        // Act & Assert
        mockMvc.perform(post("/api/flight-schedules")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    [{ "periodFrom": "2026-01-05", "periodTo": "2026-01-18", "weekdays": ["MONDAY"],
                       "departureTime": "06:30", "arrivalTime": "09:45", "flightNumber": "HJ221",
                       "departureAirport": "BRU", "arrivalAirport": "ATH", "registration": "SXBAA" }]
                    """))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.detail").value(startsWith("RabbitMQ cannot be reached")));
    }
}
