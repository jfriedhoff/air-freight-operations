package be.kdg.airfreight.messaging.api;

import be.kdg.airfreight.messaging.domain.DomainRuleViolationException;
import be.kdg.airfreight.messaging.domain.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorHandlingTest {
    private final ErrorHandling errorHandling = new ErrorHandling();

    @Test
    void aMissingResourceIsReportedAsNotFoundWithItsReason() {
        ProblemDetail problem = errorHandling.notFound(new NotFoundException("Not found: HJ221"));

        assertThat(problem.getStatus()).isEqualTo(404);
        assertThat(problem.getDetail()).isEqualTo("Not found: HJ221");
    }

    @Test
    void aBrokenDomainRuleIsReportedAsConflictWithItsReason() {
        ProblemDetail problem = errorHandling.domainRuleViolated(new DomainRuleViolationException("Rule broken"));

        assertThat(problem.getStatus()).isEqualTo(409);
        assertThat(problem.getDetail()).isEqualTo("Rule broken");
    }
}
