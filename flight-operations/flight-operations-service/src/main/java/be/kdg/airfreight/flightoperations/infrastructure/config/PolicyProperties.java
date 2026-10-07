package be.kdg.airfreight.flightoperations.infrastructure.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** The policy values the operations manager can change without a release. */
@ConfigurationProperties("flight-operations.policy")
record PolicyProperties(Duration actualTimeTolerance, int retentionDays) {
}
