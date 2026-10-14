package be.kdg.airfreight.loadplanning.infrastructure.flightcargo.jpa;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaFlightCargoRepository extends JpaRepository<JpaFlightCargo, UUID> {
    @Query("SELECT c FROM JpaFlightCargo c LEFT JOIN FETCH c.ulds WHERE c.id = :id")
    Optional<JpaFlightCargo> findWithUldsById(@Param("id") UUID id);

    @Query("""
        SELECT DISTINCT c FROM JpaFlightCargo c LEFT JOIN FETCH c.ulds
        WHERE c.scheduledDepartureDate = :scheduledDepartureDate
        ORDER BY c.std, c.flightNumber""")
    List<JpaFlightCargo> findWithUldsByScheduledDepartureDate(
        @Param("scheduledDepartureDate") LocalDate scheduledDepartureDate);

    @Query("""
        SELECT c FROM JpaFlightCargo c LEFT JOIN FETCH c.ulds
        WHERE c.flightNumber = :flightNumber AND c.scheduledDepartureDate = :scheduledDepartureDate
          AND c.departureAirport = :departureAirport""")
    Optional<JpaFlightCargo> findWithUldsByFlight(@Param("flightNumber") String flightNumber,
                                                  @Param("scheduledDepartureDate") LocalDate scheduledDepartureDate,
                                                  @Param("departureAirport") String departureAirport);
}
