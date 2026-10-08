package be.kdg.airfreight.flightoperations.api;

import be.kdg.airfreight.flightoperations.domain.DomainRuleViolationException;
import be.kdg.airfreight.flightoperations.domain.InvalidValueException;
import be.kdg.airfreight.flightoperations.domain.NotFoundException;
import be.kdg.airfreight.flightoperations.domain.flightplanner.UnknownFlightPlannerException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ProblemDetail;
import org.springframework.transaction.CannotCreateTransactionException;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorHandlingTest {
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
            new CannotCreateTransactionException("Connection to localhost:5443 refused"));

        assertProblem(problem, 503, "database-unavailable", "The service is temporarily unavailable: try again later");
    }

    private static void assertProblem(ProblemDetail problem, int status, String code, String detail) {
        assertThat(problem.getStatus()).isEqualTo(status);
        assertThat(problem.getProperties()).containsEntry("code", code);
        assertThat(problem.getDetail()).isEqualTo(detail);
    }
}
