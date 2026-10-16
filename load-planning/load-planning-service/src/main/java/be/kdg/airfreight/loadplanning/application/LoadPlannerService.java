package be.kdg.airfreight.loadplanning.application;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

import be.kdg.airfreight.loadplanning.domain.loadplanner.LoadPlanner;
import be.kdg.airfreight.loadplanning.domain.loadplanner.LoadPlannerId;
import be.kdg.airfreight.loadplanning.domain.loadplanner.LoadPlannerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keycloak decides who is a load planner. This service keeps its own copy of each planner, with the name
 * from the token, so that a new colleague can work without a restart and a change can show who made it.
 * It runs in its own transaction before the change, so each transaction still writes one aggregate.
 */
@Service
@Transactional
public class LoadPlannerService {
    private final LoadPlannerRepository loadPlanners;

    public LoadPlannerService(LoadPlannerRepository loadPlanners) {
        this.loadPlanners = loadPlanners;
    }

    /** Saves a planner seen for the first time, and the new name when it was changed in Keycloak. */
    public void register(LoadPlannerId id, String name) {
        Optional<LoadPlanner> known = loadPlanners.findById(id);
        if (known.isEmpty() || !known.get().getName().equals(name)) {
            loadPlanners.save(new LoadPlanner(id, name));
        }
    }

    /** The names of these planners, so the cargo page can show who changed the load. */
    @Transactional(readOnly = true)
    public Map<LoadPlannerId, String> namesOf(Collection<LoadPlannerId> ids) {
        Map<LoadPlannerId, String> names = new HashMap<>();
        for (LoadPlannerId id : new HashSet<>(ids)) {
            loadPlanners.findById(id).ifPresent(planner -> names.put(id, planner.getName()));
        }
        return names;
    }
}
