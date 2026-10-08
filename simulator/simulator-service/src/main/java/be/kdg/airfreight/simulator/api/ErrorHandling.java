package be.kdg.airfreight.simulator.api;

import java.util.Objects;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

/**
 * Says why a request is refused. Ordered first: Spring Boot's own advice for problem details answers
 * these exceptions too, but without the reason.
 */
@Slf4j
@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class ErrorHandling {
    /** Broken JSON, or a value of the wrong type. The first line of the parser's message is the reason. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail unreadableJson(HttpMessageNotReadableException e) {
        String message = Objects.requireNonNullElse(e.getMostSpecificCause().getMessage(), "");
        String reason = message.lines().findFirst().orElse("");
        log.warn("Refused JSON that cannot be read: {}", reason);
        return problem(HttpStatus.BAD_REQUEST, "The JSON cannot be read: " + reason);
    }

    /** A missing field, a null record or an empty list: the messages of the constraints, each once. */
    @ExceptionHandler(HandlerMethodValidationException.class)
    ProblemDetail invalidRecords(HandlerMethodValidationException e) {
        String reasons = e.getAllErrors().stream()
            .map(MessageSourceResolvable::getDefaultMessage)
            .distinct()
            .collect(Collectors.joining("; "));
        log.warn("Refused incomplete records: {}", reasons);
        return problem(HttpStatus.BAD_REQUEST, reasons);
    }

    /**
     * The broker cannot be reached. The simulator has no outbox: the trainer sends again. That is
     * safe, because a flight or a booking that already exists is not created a second time.
     */
    @ExceptionHandler(AmqpException.class)
    ProblemDetail streamUnavailable(AmqpException e) {
        log.error("RabbitMQ cannot be reached, so not every record was sent: {}", e.getMessage());
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "RabbitMQ cannot be reached, so not every record was sent. "
            + "Supply the records again once it is back: what was already sent is not created twice.");
    }

    private static ProblemDetail problem(HttpStatusCode status, String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(status);
        problemDetail.setDetail(detail);
        return problemDetail;
    }
}
