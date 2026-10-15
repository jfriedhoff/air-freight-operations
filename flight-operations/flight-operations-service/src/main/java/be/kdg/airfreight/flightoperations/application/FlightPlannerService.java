package be.kdg.airfreight.flightoperations.application;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

import be.kdg.airfreight.flightoperations.domain.flightplanner.FlightPlanner;
import be.kdg.airfreight.flightoperations.domain.flightplanner.FlightPlannerId;
import be.kdg.airfreight.flightoperations.domain.flightplanner.FlightPlannerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keycloak decides who is a flight planner. This service keeps its own copy of each planner, with the name
 * from the token, so that a new colleague can work without a restart and a change can show who made it.
 * It runs in its own transaction before the change, so each transaction still writes one aggregate.
 */
@Service
@Transactional
public class FlightPlannerService {
    private final FlightPlannerRepository flightPlanners;

    public FlightPlannerService(FlightPlannerRepository flightPlanners) {
        this.flightPlanners = flightPlanners;
    }

    /** Saves a planner seen for the first time, and the new name when it was changed in Keycloak. */
    public void register(FlightPlannerId id, String name) {
        Optional<FlightPlanner> known = flightPlanners.findById(id);
        if (known.isEmpty() || !known.get().getName().equals(name)) {
            flightPlanners.save(new FlightPlanner(id, name));
        }
    }

    /** The names of these planners, so the details page can show who changed the flight. */
    @Transactional(readOnly = true)
    public Map<FlightPlannerId, String> namesOf(Collection<FlightPlannerId> ids) {
        Map<FlightPlannerId, String> names = new HashMap<>();
        for (FlightPlannerId id : new HashSet<>(ids)) {
            flightPlanners.findById(id).ifPresent(planner -> names.put(id, planner.getName()));
        }
        return names;
    }
}
