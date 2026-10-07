package be.kdg.airfreight.flightoperations.infrastructure.config;

import java.time.Clock;

import be.kdg.airfreight.flightoperations.domain.flight.ActualTimeTolerance;
import be.kdg.airfreight.flightoperations.domain.flight.RetentionPeriod;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Hands the configured policy values to the domain as its own value objects. */
@Configuration
@EnableConfigurationProperties(PolicyProperties.class)
class PolicyConfig {
    @Bean
    ActualTimeTolerance actualTimeTolerance(PolicyProperties policy) {
        return new ActualTimeTolerance(policy.actualTimeTolerance());
    }

    @Bean
    RetentionPeriod retentionPeriod(PolicyProperties policy) {
        return new RetentionPeriod(policy.retentionDays());
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
