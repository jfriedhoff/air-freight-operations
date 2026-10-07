package be.kdg.airfreight.loadplanning.infrastructure.config;

import java.math.BigDecimal;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** The policy values the operations manager can change without a release. */
@ConfigurationProperties("load-planning.policy")
record PolicyProperties(Duration loadingOpensBeforeDeparture, Duration loadingClosesBeforeDeparture,
                        BigDecimal lateralBalanceLimit) {
}
