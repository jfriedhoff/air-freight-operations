package be.kdg.airfreight.flightoperations.infrastructure.flight.jpa;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaFlightRepository extends JpaRepository<JpaFlight, UUID> {
    @Query("SELECT f FROM JpaFlight f LEFT JOIN FETCH f.delays WHERE f.id = :id")
    Optional<JpaFlight> findWithDelaysById(@Param("id") UUID id);

    @Query("""
        SELECT f FROM JpaFlight f LEFT JOIN FETCH f.delays
        WHERE f.flightNumber = :flightNumber AND f.scheduledDepartureDate = :scheduledDepartureDate
          AND f.departureAirport = :departureAirport""")
    Optional<JpaFlight> findWithDelaysByFlight(@Param("flightNumber") String flightNumber,
                                               @Param("scheduledDepartureDate") LocalDate scheduledDepartureDate,
                                               @Param("departureAirport") String departureAirport);

    @Query("""
        SELECT DISTINCT f FROM JpaFlight f LEFT JOIN FETCH f.delays
        WHERE f.scheduledDepartureDate = :scheduledDepartureDate
        ORDER BY f.std, f.flightNumber""")
    List<JpaFlight> findWithDelaysByScheduledDepartureDate(
        @Param("scheduledDepartureDate") LocalDate scheduledDepartureDate);

    @Query("""
        SELECT DISTINCT f FROM JpaFlight f LEFT JOIN FETCH f.delays
        WHERE f.scheduledDepartureDate <= :scheduledDepartureDate""")
    List<JpaFlight> findWithDelaysScheduledOnOrBefore(@Param("scheduledDepartureDate") LocalDate scheduledDepartureDate);
}
