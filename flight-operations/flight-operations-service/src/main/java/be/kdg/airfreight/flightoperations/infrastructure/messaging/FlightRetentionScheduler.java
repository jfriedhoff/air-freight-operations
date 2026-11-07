package be.kdg.airfreight.flightoperations.infrastructure.messaging;

import be.kdg.airfreight.flightoperations.application.FlightRetentionService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Time is the actor here: every day at 00:00 UTC the flights past their retention period are removed. A unit
 * that was not running at midnight missed that run, so the same job also runs once when the unit has started.
 */
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

    @EventListener(ApplicationReadyEvent.class)
    void removeExpiredFlightsAtStartup() {
        retention.removeExpiredFlights();
    }
}
