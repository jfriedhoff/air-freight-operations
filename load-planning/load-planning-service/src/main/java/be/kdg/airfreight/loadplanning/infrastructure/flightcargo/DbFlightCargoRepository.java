package be.kdg.airfreight.loadplanning.infrastructure.flightcargo;

import java.util.Optional;

import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargo;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargoId;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargoRepository;
import be.kdg.airfreight.loadplanning.infrastructure.flightcargo.jpa.JpaFlightCargo;
import be.kdg.airfreight.loadplanning.infrastructure.flightcargo.jpa.JpaFlightCargoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Profile("db")
class DbFlightCargoRepository implements FlightCargoRepository {
    private final JpaFlightCargoRepository jpaFlightCargos;
    private final EntityManager entityManager;

    DbFlightCargoRepository(JpaFlightCargoRepository jpaFlightCargos, EntityManager entityManager) {
        this.jpaFlightCargos = jpaFlightCargos;
        this.entityManager = entityManager;
    }

    @Override
    public Optional<FlightCargo> findById(FlightCargoId id) {
        return jpaFlightCargos.findWithUldsById(id.id()).map(JpaFlightCargo::toDomain);
    }

    @Override
    @Transactional
    public void save(FlightCargo flightCargo) {
        JpaFlightCargo saved = jpaFlightCargos.save(JpaFlightCargo.fromDomain(flightCargo));
        if (flightCargo.getVersion() != null) {
            // A changed ULD does not touch the flight_cargos row: raise the version of the aggregate anyway.
            entityManager.lock(saved, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        }
    }
}
