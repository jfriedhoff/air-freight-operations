package be.kdg.airfreight.flightoperations.api;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * The last resort: an error that nothing else answers is a fault of ours. It is logged with its stack trace, and
 * the caller gets Problem Details without internal details.
 * A separate advice with the lowest precedence: Spring's own Problem Details handler comes first, so an unknown
 * path stays 404 and a wrong verb 405. In ErrorHandling this handler would catch those too.
 */
@Slf4j
@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
class UnexpectedErrorHandling {
    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception e, HttpServletRequest request) {
        log.error("Unexpected error for {} {}", request.getMethod(), request.getRequestURI(), e);
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problemDetail.setDetail("Something went wrong on our side: try again later");
        problemDetail.setProperty("code", "unexpected-error");
        return problemDetail;
    }
}
