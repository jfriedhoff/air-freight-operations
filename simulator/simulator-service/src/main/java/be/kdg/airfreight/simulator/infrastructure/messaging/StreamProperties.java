package be.kdg.airfreight.simulator.infrastructure.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** The exchange of each stream the simulator imitates. */
@ConfigurationProperties("simulator")
public record StreamProperties(String scheduleExchange, String bookingsExchange) {
}
