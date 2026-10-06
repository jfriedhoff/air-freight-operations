package be.kdg.airfreight.flightoperations.infrastructure.flight;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import be.kdg.airfreight.flightoperations.domain.flight.AirportCode;
import be.kdg.airfreight.flightoperations.domain.flight.Flight;
import be.kdg.airfreight.flightoperations.domain.flight.FlightId;
import be.kdg.airfreight.flightoperations.domain.flight.FlightNumber;
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
    public Optional<Flight> findByFlight(FlightNumber flightNumber, LocalDate scheduledDepartureDate,
                                         AirportCode departureAirport) {
        return jpaFlights.findWithDelaysByFlight(flightNumber.value(), scheduledDepartureDate,
            departureAirport.value()).map(JpaFlight::toDomain);
    }

    @Override
    public List<Flight> findScheduledOnOrBefore(LocalDate scheduledDepartureDate) {
        return jpaFlights.findWithDelaysScheduledOnOrBefore(scheduledDepartureDate).stream()
            .map(JpaFlight::toDomain)
            .toList();
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

    /** Deleting the flight deletes its delays with it: the aggregate goes as a whole. */
    @Override
    @Transactional
    public void delete(Flight flight) {
        jpaFlights.deleteById(flight.getId().id());
    }
}
