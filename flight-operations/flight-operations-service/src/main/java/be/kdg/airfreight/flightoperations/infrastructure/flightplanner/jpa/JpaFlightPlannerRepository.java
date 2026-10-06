package be.kdg.airfreight.flightoperations.infrastructure.flightplanner.jpa;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaFlightPlannerRepository extends JpaRepository<JpaFlightPlanner, UUID> {
}
