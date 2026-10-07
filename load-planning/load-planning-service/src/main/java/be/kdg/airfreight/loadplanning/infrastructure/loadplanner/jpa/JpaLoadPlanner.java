package be.kdg.airfreight.loadplanning.infrastructure.loadplanner.jpa;

import java.util.UUID;

import be.kdg.airfreight.loadplanning.domain.loadplanner.LoadPlanner;
import be.kdg.airfreight.loadplanning.domain.loadplanner.LoadPlannerId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "load_planners")
public class JpaLoadPlanner {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    protected JpaLoadPlanner() {
    }

    private JpaLoadPlanner(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

    public static JpaLoadPlanner fromDomain(LoadPlanner loadPlanner) {
        return new JpaLoadPlanner(loadPlanner.getId().id(), loadPlanner.getName());
    }

    public LoadPlanner toDomain() {
        return new LoadPlanner(new LoadPlannerId(id), name);
    }
}
