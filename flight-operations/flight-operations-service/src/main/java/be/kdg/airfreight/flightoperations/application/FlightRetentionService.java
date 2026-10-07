package be.kdg.airfreight.flightoperations.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import be.kdg.airfreight.flightoperations.domain.flight.Flight;
import be.kdg.airfreight.flightoperations.domain.flight.FlightRepository;
import be.kdg.airfreight.flightoperations.domain.flight.RetentionPeriod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class FlightRetentionService {
    private final FlightRepository flights;
    private final ApplicationEventPublisher events;
    private final RetentionPeriod retention;
    private final Clock clock;

    public FlightRetentionService(FlightRepository flights, ApplicationEventPublisher events,
                                  RetentionPeriod retention, Clock clock) {
        this.flights = flights;
        this.events = events;
        this.retention = retention;
        this.clock = clock;
    }

    /** Deletes every flight whose retention period is over, whatever its state, and announces each removal. */
    @Transactional
    public void removeExpiredFlights() {
        Instant now = clock.instant();
        LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);
        List<Flight> expired = flights.findScheduledOnOrBefore(retention.lastExpiredDate(today));
        for (Flight flight : expired) {
            flights.delete(flight);
            events.publishEvent(flight.removed(now));
            log.info("Flight {} of {} removed after its retention period", flight.getFlightNumber().value(),
                flight.getScheduled().departureDate());
        }
    }
}
