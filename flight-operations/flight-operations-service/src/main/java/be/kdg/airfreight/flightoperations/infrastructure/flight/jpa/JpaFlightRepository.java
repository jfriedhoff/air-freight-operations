package be.kdg.airfreight.flightoperations.infrastructure.flight.jpa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaFlightRepository extends JpaRepository<JpaFlight, UUID> {
    @Query("SELECT f FROM JpaFlight f LEFT JOIN FETCH f.delays WHERE f.id = :id")
    Optional<JpaFlight> findWithDelaysById(@Param("id") UUID id);
}
