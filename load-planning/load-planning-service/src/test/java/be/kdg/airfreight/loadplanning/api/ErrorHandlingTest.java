package be.kdg.airfreight.loadplanning.api;

import be.kdg.airfreight.loadplanning.domain.DomainRuleViolationException;
import be.kdg.airfreight.loadplanning.domain.InvalidValueException;
import be.kdg.airfreight.loadplanning.domain.NotFoundException;
import be.kdg.airfreight.loadplanning.domain.loadplanner.UnknownLoadPlannerException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorHandlingTest {
    private static final MockHttpServletRequest REQUEST = new MockHttpServletRequest("GET", "/api/42");

    private final ErrorHandling errorHandling = new ErrorHandling();

    @Test
    void aValueThatIsInvalidInItselfIsABadRequestWithItsReason() {
        ProblemDetail problem = errorHandling.invalidValue(new InvalidValueException("ULD code 'AKE1' is invalid"));

        assertProblem(problem, 400, "invalid-input", "ULD code 'AKE1' is invalid");
    }

    @Test
    void anUnknownLoadPlannerIsForbidden() {
        ProblemDetail problem = errorHandling.unknownLoadPlanner(
            new UnknownLoadPlannerException("You are not registered as a load planner"));

        assertProblem(problem, 403, "unknown-load-planner", "You are not registered as a load planner");
    }

    @Test
    void aMissingResourceIsReportedAsNotFoundWithItsReason() {
        ProblemDetail problem = errorHandling.notFound(new NotFoundException("Not found: HJ221"));

        assertProblem(problem, 404, "not-found", "Not found: HJ221");
    }

    @Test
    void anActionThatDoesNotFitTheStateIsAConflictWithItsReason() {
        ProblemDetail problem = errorHandling.ruleRefused(
            new DomainRuleViolationException("The load is finalized and can no longer change"));

        assertProblem(problem, 409, "rule-refused", "The load is finalized and can no longer change");
    }

    @Test
    void aConcurrentChangeIsAConflictThatAsksToTryAgain() {
        ProblemDetail problem = errorHandling.concurrentChange(
            new OptimisticLockingFailureException("Row was updated or deleted by another transaction"));

        assertProblem(problem, 409, "concurrent-change",
            "Someone else changed the load of this flight at the same time: reload it and try again");
    }

    @Test
    void anUnreachableDatabaseIsServiceUnavailableWithoutInternalDetails() {
        ProblemDetail problem = errorHandling.databaseUnavailable(
            new DataAccessResourceFailureException("Connection to localhost:5443 refused"), REQUEST);

        assertProblem(problem, 503, "database-unavailable", "The service is temporarily unavailable: try again later");
    }

    /** The reason of an invalid value can repeat what the caller typed: in the log it stays on one line. */
    @Test
    void aReasonThatRepeatsWhatTheCallerSentIsLoggedOnOneLine() {
        assertThat(ErrorHandling.onOneLine("Load position CL\r\n2026-01-09 INFO forged log line is not on the floor plan"))
            .isEqualTo("Load position CL__2026-01-09 INFO forged log line is not on the floor plan");
        assertThat(ErrorHandling.onOneLine(null)).isEmpty();
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
