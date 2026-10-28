package be.kdg.airfreight.flightoperations;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TestConfigurationTest {
    @Autowired
    private ApplicationContext context;

    @Value("${spring.datasource.url}")
    private String databaseUrl;

    @Test
    void testsUseTheirOwnDatabase() {
        assertThat(databaseUrl).endsWith("/flight_operations_test");
    }

    /** The bean of OutboxRelayScheduler, which is package-private in infrastructure.messaging. */
    @Test
    void testsPublishNothingOnTheirOwn() {
        assertThat(context.containsBean("outboxRelayScheduler")).isFalse();
    }
}
