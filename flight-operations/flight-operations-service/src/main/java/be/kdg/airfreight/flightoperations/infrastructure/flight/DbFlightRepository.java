package be.kdg.airfreight.flightoperations.infrastructure.flight;

import java.util.Optional;

import be.kdg.airfreight.flightoperations.domain.flight.Flight;
import be.kdg.airfreight.flightoperations.domain.flight.FlightId;
import be.kdg.airfreight.flightoperations.domain.flight.FlightRepository;
import be.kdg.airfreight.flightoperations.infrastructure.flight.jpa.JpaFlight;
import be.kdg.airfreight.flightoperations.infrastructure.flight.jpa.JpaFlightRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Profile("db")
class DbFlightRepository implements FlightRepository {
    private final JpaFlightRepository jpaFlights;
    private final EntityManager entityManager;

    DbFlightRepository(JpaFlightRepository jpaFlights, EntityManager entityManager) {
        this.jpaFlights = jpaFlights;
        this.entityManager = entityManager;
    }

    @Override
    public Optional<Flight> findById(FlightId id) {
        return jpaFlights.findWithDelaysById(id.id()).map(JpaFlight::toDomain);
    }

    @Override
    @Transactional
    public void save(Flight flight) {
        JpaFlight saved = jpaFlights.save(JpaFlight.fromDomain(flight));
        if (flight.getVersion() != null) {
            // A changed delay does not touch the flights row: raise the version of the aggregate anyway.
            entityManager.lock(saved, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        }
    }
}
