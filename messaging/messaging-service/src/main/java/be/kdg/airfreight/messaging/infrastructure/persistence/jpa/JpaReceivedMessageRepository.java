package be.kdg.airfreight.messaging.infrastructure.persistence.jpa;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaReceivedMessageRepository extends JpaRepository<JpaReceivedMessage, UUID> {
    void deleteByFlightNumberAndScheduledDepartureDateAndDepartureAirport(
        String flightNumber, LocalDate scheduledDepartureDate, String departureAirport);
}
