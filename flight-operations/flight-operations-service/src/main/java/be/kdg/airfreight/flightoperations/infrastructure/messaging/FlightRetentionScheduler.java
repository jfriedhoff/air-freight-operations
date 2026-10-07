package be.kdg.airfreight.flightoperations.infrastructure.messaging;

import be.kdg.airfreight.flightoperations.application.FlightRetentionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Time is the actor here: every day at 00:00 UTC the flights past their retention period are removed. */
@Component
class FlightRetentionScheduler {
    private final FlightRetentionService retention;

    FlightRetentionScheduler(FlightRetentionService retention) {
        this.retention = retention;
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "UTC")
    void removeExpiredFlights() {
        retention.removeExpiredFlights();
    }
}
