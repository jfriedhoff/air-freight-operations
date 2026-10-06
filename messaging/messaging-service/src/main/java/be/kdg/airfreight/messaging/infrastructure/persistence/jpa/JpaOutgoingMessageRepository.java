package be.kdg.airfreight.messaging.infrastructure.persistence.jpa;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaOutgoingMessageRepository extends JpaRepository<JpaOutgoingMessage, UUID> {
    List<JpaOutgoingMessage> findBySentAtIsNullOrderByCreatedAtAsc();

    void deleteByFlightNumberAndScheduledDepartureDateAndDepartureAirport(
        String flightNumber, LocalDate scheduledDepartureDate, String departureAirport);
}
