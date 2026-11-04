package be.kdg.airfreight.messaging.infrastructure.persistence.jpa;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaOutgoingMessageRepository extends JpaRepository<JpaOutgoingMessage, UUID> {
    List<JpaOutgoingMessage> findBySentAtIsNullOrderByCreatedAtAsc();

    /** Changes nothing when the row is gone, unlike save. */
    @Modifying
    @Query("update JpaOutgoingMessage m set m.sentAt = :sentAt where m.id = :id")
    int markSent(@Param("id") UUID id, @Param("sentAt") Instant sentAt);

    void deleteByFlightNumberAndScheduledDepartureDateAndDepartureAirport(
        String flightNumber, LocalDate scheduledDepartureDate, String departureAirport);
}
