package be.kdg.airfreight.loadplanning.infrastructure.loadplanner.jpa;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaLoadPlannerRepository extends JpaRepository<JpaLoadPlanner, UUID> {
}
