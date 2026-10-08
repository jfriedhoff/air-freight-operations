package be.kdg.airfreight.simulator.api;

import java.net.ConnectException;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorHandlingTest {
    private final ErrorHandling errorHandling = new ErrorHandling();

    @Test
    void unreadableJsonIsReportedAsBadRequestWithTheFirstLineOfTheReason() {
        HttpMessageNotReadableException unreadable = new HttpMessageNotReadableException("JSON parse error",
            new IllegalArgumentException("Unexpected end-of-input\n at [line: 1, column: 20]"),
            new MockHttpInputMessage(new byte[0]));

        ProblemDetail problem = errorHandling.unreadableJson(unreadable);

        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getDetail()).isEqualTo("The JSON cannot be read: Unexpected end-of-input");
    }

    @Test
    void anUnreachableBrokerIsReportedAsServiceUnavailable() {
        ProblemDetail problem = errorHandling.streamUnavailable(
            new AmqpConnectException(new ConnectException("Connection refused")));

        assertThat(problem.getStatus()).isEqualTo(503);
        assertThat(problem.getDetail()).startsWith("RabbitMQ cannot be reached");
    }
}
