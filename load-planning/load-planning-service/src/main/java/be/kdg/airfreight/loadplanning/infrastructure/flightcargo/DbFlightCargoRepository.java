package be.kdg.airfreight.loadplanning.infrastructure.flightcargo;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargo;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargoId;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargoRepository;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightId;
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
    public Optional<FlightCargo> findByFlightId(FlightId flightId) {
        return jpaFlightCargos.findWithUldsByFlight(flightId.flightNumber().value(),
            flightId.scheduledDepartureDate(), flightId.departureAirport().value()).map(JpaFlightCargo::toDomain);
    }

    @Override
    public List<FlightCargo> findByScheduledDepartureDate(LocalDate scheduledDepartureDate) {
        return jpaFlightCargos.findWithUldsByScheduledDepartureDate(scheduledDepartureDate).stream()
            .map(JpaFlightCargo::toDomain)
            .toList();
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

    /** Deleting the cargo deletes its ULDs with it: the aggregate goes as a whole. */
    @Override
    @Transactional
    public void delete(FlightCargo flightCargo) {
        jpaFlightCargos.deleteById(flightCargo.getId().id());
    }
}
