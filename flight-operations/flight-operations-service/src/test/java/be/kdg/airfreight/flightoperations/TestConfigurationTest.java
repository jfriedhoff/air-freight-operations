package be.kdg.airfreight.flightoperations;

import be.kdg.airfreight.flightoperations.infrastructure.messaging.OutboxRelay;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TestConfigurationTest {
    @Autowired
    private ObjectProvider<OutboxRelay> relay;

    @Value("${spring.datasource.url}")
    private String databaseUrl;

    @Test
    void testsUseTheirOwnDatabase() {
        assertThat(databaseUrl).endsWith("/flight_operations_test");
    }

    @Test
    void testsPublishNothing() {
        assertThat(relay.getIfAvailable()).isNull();
    }
}
