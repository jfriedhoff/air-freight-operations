package be.kdg.airfreight.flightoperations.api;

import be.kdg.airfreight.flightoperations.domain.DomainRuleViolationException;
import be.kdg.airfreight.flightoperations.domain.InvalidValueException;
import be.kdg.airfreight.flightoperations.domain.NotFoundException;
import be.kdg.airfreight.flightoperations.domain.flightplanner.UnknownFlightPlannerException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.CannotCreateTransactionException;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorHandlingTest {
    private static final MockHttpServletRequest REQUEST = new MockHttpServletRequest("GET", "/api/42");

    private final ErrorHandling errorHandling = new ErrorHandling();

    @Test
    void aValueThatIsInvalidInItselfIsABadRequestWithItsReason() {
        ProblemDetail problem = errorHandling.invalidValue(new InvalidValueException("Delay too long"));

        assertProblem(problem, 400, "invalid-input", "Delay too long");
    }

    @Test
    void anUnknownFlightPlannerIsForbidden() {
        ProblemDetail problem = errorHandling.unknownFlightPlanner(
            new UnknownFlightPlannerException("You are not registered as a flight planner"));

        assertProblem(problem, 403, "unknown-flight-planner", "You are not registered as a flight planner");
    }

    @Test
    void aMissingResourceIsReportedAsNotFoundWithItsReason() {
        ProblemDetail problem = errorHandling.notFound(new NotFoundException("Not found: HJ221"));

        assertProblem(problem, 404, "not-found", "Not found: HJ221");
    }

    @Test
    void anActionThatDoesNotFitTheStateIsAConflictWithItsReason() {
        ProblemDetail problem = errorHandling.ruleRefused(new DomainRuleViolationException("Rule broken"));

        assertProblem(problem, 409, "rule-refused", "Rule broken");
    }

    @Test
    void aConcurrentChangeIsAConflictThatAsksToTryAgain() {
        ProblemDetail problem = errorHandling.concurrentChange(new OptimisticLockingFailureException("row 12"));

        assertProblem(problem, 409, "concurrent-change",
            "Someone else changed this flight at the same time: reload it and try again");
    }

    @Test
    void anUnreachableDatabaseIsServiceUnavailableWithoutInternalDetails() {
        ProblemDetail problem = errorHandling.databaseUnavailable(
            new CannotCreateTransactionException("Connection to localhost:5443 refused"), REQUEST);

        assertProblem(problem, 503, "database-unavailable", "The service is temporarily unavailable: try again later");
    }

    @Test
    void anotherDatabaseErrorIsAnInternalErrorWithoutInternalDetails() {
        ProblemDetail problem = errorHandling.databaseError(
            new DataIntegrityViolationException("duplicate key value violates unique constraint \"uk_flight\""), REQUEST);

        assertProblem(problem, 500, "database-error", "The service could not complete the request: try again later");
    }

    @Test
    void anUnexpectedErrorIsAnInternalErrorWithoutInternalDetails() {
        ProblemDetail problem = new UnexpectedErrorHandling().unexpected(new NullPointerException("flight is null"), REQUEST);

        assertProblem(problem, 500, "unexpected-error", "Something went wrong on our side: try again later");
    }

    private static void assertProblem(ProblemDetail problem, int status, String code, String detail) {
        assertThat(problem.getStatus()).isEqualTo(status);
        assertThat(problem.getProperties()).containsEntry("code", code);
        assertThat(problem.getDetail()).isEqualTo(detail);
    }
}
