package be.kdg.airfreight.flightoperations.infrastructure.flightplanner.jpa;

import java.util.UUID;

import be.kdg.airfreight.flightoperations.domain.flightplanner.FlightPlanner;
import be.kdg.airfreight.flightoperations.domain.flightplanner.FlightPlannerId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "flight_planners")
public class JpaFlightPlanner {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    protected JpaFlightPlanner() {
    }

    private JpaFlightPlanner(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

    public static JpaFlightPlanner fromDomain(FlightPlanner flightPlanner) {
        return new JpaFlightPlanner(flightPlanner.getId().id(), flightPlanner.getName());
    }

    public FlightPlanner toDomain() {
        return new FlightPlanner(new FlightPlannerId(id), name);
    }
}
