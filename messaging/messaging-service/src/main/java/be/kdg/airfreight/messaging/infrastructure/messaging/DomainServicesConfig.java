package be.kdg.airfreight.messaging.infrastructure.messaging;

import java.time.Clock;

import be.kdg.airfreight.messaging.domain.telegram.AaWriter;
import be.kdg.airfreight.messaging.domain.telegram.AdWriter;
import be.kdg.airfreight.messaging.domain.telegram.DateDerivation;
import be.kdg.airfreight.messaging.domain.telegram.EaWriter;
import be.kdg.airfreight.messaging.domain.telegram.EdWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class DomainServicesConfig {
    @Bean
    EdWriter edWriter() {
        return new EdWriter();
    }

    @Bean
    EaWriter eaWriter() {
        return new EaWriter();
    }

    @Bean
    AdWriter adWriter() {
        return new AdWriter();
    }

    @Bean
    AaWriter aaWriter() {
        return new AaWriter();
    }

    @Bean
    DateDerivation dateDerivation() {
        return new DateDerivation();
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
