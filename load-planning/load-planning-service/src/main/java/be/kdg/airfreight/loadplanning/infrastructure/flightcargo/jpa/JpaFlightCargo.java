package be.kdg.airfreight.loadplanning.infrastructure.flightcargo.jpa;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import be.kdg.airfreight.loadplanning.domain.flightcargo.AircraftType;
import be.kdg.airfreight.loadplanning.domain.flightcargo.AirportCode;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargo;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargoId;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightId;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightInfo;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightNumber;
import be.kdg.airfreight.loadplanning.domain.loadplanner.LoadPlannerId;
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
@Table(name = "flight_cargos", uniqueConstraints = @UniqueConstraint(
    name = "uk_flight_cargos_number_date_departure",
    columnNames = {"flight_number", "scheduled_departure_date", "departure_airport"}))
public class JpaFlightCargo {
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
    private String aircraftType;

    @Column(nullable = false)
    private Instant std;

    @Column(nullable = false)
    private Instant sta;

    @Column(nullable = false)
    private Instant etd;

    @Column(nullable = false)
    private Instant eta;

    @Column(nullable = false)
    private boolean departed;

    @Column(nullable = false)
    private Instant loadingClosesAt;

    @Column(nullable = false)
    private boolean finalized;

    private @Nullable UUID finalizedBy;

    @OneToMany(mappedBy = "flightCargo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JpaUld> ulds = new ArrayList<>();

    protected JpaFlightCargo() {
    }

    UUID getId() {
        return id;
    }

    public static JpaFlightCargo fromDomain(FlightCargo cargo) {
        JpaFlightCargo jpaCargo = new JpaFlightCargo();
        jpaCargo.id = cargo.getId().id();
        jpaCargo.version = cargo.getVersion();
        jpaCargo.flightNumber = cargo.getFlightId().flightNumber().value();
        jpaCargo.scheduledDepartureDate = cargo.getFlightId().scheduledDepartureDate();
        jpaCargo.departureAirport = cargo.getFlightId().departureAirport().value();
        FlightInfo info = cargo.getFlightInfo();
        jpaCargo.arrivalAirport = info.arrivalAirport().value();
        jpaCargo.aircraftType = info.aircraftType().code();
        jpaCargo.std = info.std();
        jpaCargo.sta = info.sta();
        jpaCargo.etd = info.etd();
        jpaCargo.eta = info.eta();
        jpaCargo.departed = info.departed();
        jpaCargo.loadingClosesAt = cargo.getLoadingClosesAt();
        jpaCargo.finalized = cargo.isFinalized();
        LoadPlannerId finalizedBy = cargo.getFinalizedBy();
        jpaCargo.finalizedBy = finalizedBy == null ? null : finalizedBy.id();
        cargo.getUlds().forEach(uld -> jpaCargo.ulds.add(JpaUld.fromDomain(uld, jpaCargo)));
        return jpaCargo;
    }

    public FlightCargo toDomain() {
        return FlightCargo.hydrate(
            new FlightCargoId(id),
            new FlightId(new FlightNumber(flightNumber), scheduledDepartureDate, new AirportCode(departureAirport)),
            new FlightInfo(new AirportCode(arrivalAirport), new AircraftType(aircraftType), std, sta, etd, eta,
                departed),
            loadingClosesAt,
            ulds.stream().map(JpaUld::toDomain).toList(),
            finalized,
            finalizedBy == null ? null : new LoadPlannerId(finalizedBy),
            version);
    }
}
