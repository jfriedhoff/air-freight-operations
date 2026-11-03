package be.kdg.airfreight.flightoperations.infrastructure.flight.jpa;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import be.kdg.airfreight.flightoperations.domain.flight.AircraftRegistration;
import be.kdg.airfreight.flightoperations.domain.flight.AirportCode;
import be.kdg.airfreight.flightoperations.domain.flight.Flight;
import be.kdg.airfreight.flightoperations.domain.flight.FlightId;
import be.kdg.airfreight.flightoperations.domain.flight.FlightNumber;
import be.kdg.airfreight.flightoperations.domain.flight.ScheduledTimes;
import be.kdg.airfreight.flightoperations.domain.flightplanner.FlightPlannerId;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "flights", uniqueConstraints = @UniqueConstraint(
    name = "uk_flights_number_date_departure",
    columnNames = {"flight_number", "scheduled_departure_date", "departure_airport"}))
public class JpaFlight {
    @Id
    private UUID id;

    @Version
    private @Nullable Long version;

    @Column(name = "flight_number", nullable = false, length = 7)
    private String flightNumber;

    @Column(name = "scheduled_departure_date", nullable = false)
    private LocalDate scheduledDepartureDate;

    @Column(name = "departure_airport", nullable = false, length = 3)
    private String departureAirport;

    @Column(nullable = false, length = 3)
    private String arrivalAirport;

    @Column(nullable = false)
    private String registration;

    @Column(nullable = false)
    private Instant std;

    @Column(nullable = false)
    private Instant sta;

    private @Nullable Instant actualDeparture;

    private @Nullable Instant actualArrival;

    private @Nullable UUID departureRegisteredBy;

    private @Nullable UUID arrivalRegisteredBy;

    @Column(nullable = false)
    private boolean finalizedLoadReceived;

    @OneToMany(mappedBy = "flight", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JpaDelay> delays = new ArrayList<>();

    protected JpaFlight() {
    }

    public static JpaFlight fromDomain(Flight flight) {
        JpaFlight jpaFlight = new JpaFlight();
        jpaFlight.id = flight.getId().id();
        jpaFlight.version = flight.getVersion();
        jpaFlight.flightNumber = flight.getFlightNumber().value();
        jpaFlight.scheduledDepartureDate = flight.getScheduled().departureDate();
        jpaFlight.departureAirport = flight.getDepartureAirport().value();
        jpaFlight.arrivalAirport = flight.getArrivalAirport().value();
        jpaFlight.registration = flight.getRegistration().value();
        jpaFlight.std = flight.getScheduled().departure();
        jpaFlight.sta = flight.getScheduled().arrival();
        jpaFlight.actualDeparture = flight.getActualDeparture();
        jpaFlight.actualArrival = flight.getActualArrival();
        jpaFlight.departureRegisteredBy = idOf(flight.getDepartureRegisteredBy());
        jpaFlight.arrivalRegisteredBy = idOf(flight.getArrivalRegisteredBy());
        jpaFlight.finalizedLoadReceived = flight.isFinalizedLoadReceived();
        flight.getDelays().forEach(delay -> jpaFlight.delays.add(JpaDelay.fromDomain(delay, jpaFlight)));
        return jpaFlight;
    }

    public Flight toDomain() {
        return Flight.hydrate(
            new FlightId(id),
            new FlightNumber(flightNumber),
            new AirportCode(departureAirport),
            new AirportCode(arrivalAirport),
            new AircraftRegistration(registration),
            new ScheduledTimes(std, sta),
            delays.stream().map(JpaDelay::toDomain).toList(),
            actualDeparture,
            actualArrival,
            plannerOf(departureRegisteredBy),
            plannerOf(arrivalRegisteredBy),
            finalizedLoadReceived,
            version);
    }

    private static @Nullable UUID idOf(@Nullable FlightPlannerId planner) {
        return planner == null ? null : planner.id();
    }

    private static @Nullable FlightPlannerId plannerOf(@Nullable UUID id) {
        return id == null ? null : new FlightPlannerId(id);
    }
}
