package be.kdg.airfreight.loadplanning.api;

import java.util.stream.Collectors;

import be.kdg.airfreight.loadplanning.domain.DomainRuleViolationException;
import be.kdg.airfreight.loadplanning.domain.InvalidValueException;
import be.kdg.airfreight.loadplanning.domain.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Every refusal leaves as Problem Details with a code of ours and a reason the page can show.
 * Highest precedence, so the body and path errors get our code too and not Spring's default answer.
 */
@Slf4j
@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class ErrorHandling {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail invalidBody(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .sorted()
            .collect(Collectors.joining("; "));
        log.debug("Invalid request body: {}", onOneLine(detail));
        return problem(HttpStatus.BAD_REQUEST, "invalid-input", detail);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ProblemDetail unreadableRequest(Exception e) {
        log.debug("Unreadable request: {}", onOneLine(e.getMessage()));
        return problem(HttpStatus.BAD_REQUEST, "invalid-input", "The request cannot be read: check its fields");
    }

    @ExceptionHandler(InvalidValueException.class)
    ProblemDetail invalidValue(InvalidValueException e) {
        log.debug("Invalid value: {}", onOneLine(e.getMessage()));
        return problem(HttpStatus.BAD_REQUEST, "invalid-input", e.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail notFound(NotFoundException e) {
        log.warn("Request for something that does not exist: {}", onOneLine(e.getMessage()));
        return problem(HttpStatus.NOT_FOUND, "not-found", e.getMessage());
    }

    @ExceptionHandler(DomainRuleViolationException.class)
    ProblemDetail ruleRefused(DomainRuleViolationException e) {
        log.info("Action refused: {}", onOneLine(e.getMessage()));
        return problem(HttpStatus.CONFLICT, "rule-refused", e.getMessage());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail concurrentChange(OptimisticLockingFailureException e) {
        log.warn("Concurrent change: {}", onOneLine(e.getMessage()));
        return problem(HttpStatus.CONFLICT, "concurrent-change",
            "Someone else changed the load of this flight at the same time: reload it and try again");
    }

    @ExceptionHandler({CannotCreateTransactionException.class, DataAccessResourceFailureException.class})
    ProblemDetail databaseUnavailable(RuntimeException e) {
        log.error("Database unavailable", e);
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "database-unavailable",
            "The service is temporarily unavailable: try again later");
    }

    /**
     * Every reason taken from an exception goes through here before it is logged. A reason can repeat what the
     * caller sent, and a line break in it would start a line of the caller's own in the log, so every ASCII
     * control character becomes an underscore. The answer to the caller keeps the reason as it is.
     * Unicode line separators (U+0085, U+2028) pass: a console or file appender does not break a line on them.
     */
    static String onOneLine(@Nullable String reason) {
        return reason == null ? "" : reason.replaceAll("\\p{Cntrl}", "_");
    }

    private static ProblemDetail problem(HttpStatusCode status, String code, String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(status);
        problemDetail.setDetail(detail);
        problemDetail.setProperty("code", code);
        return problemDetail;
    }
}
