package be.kdg.airfreight.loadplanning.domain.loadplanner;

import java.util.Objects;
import java.util.UUID;

import be.kdg.airfreight.loadplanning.domain.NotFoundException;
import org.jspecify.annotations.Nullable;
import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
public record LoadPlannerId(UUID id) {
    private static final String NOT_REGISTERED = "You are not registered as a load planner";

    public LoadPlannerId {
        Objects.requireNonNull(id, "Id cannot be null");
    }

    public static LoadPlannerId create() {
        return new LoadPlannerId(UUID.randomUUID());
    }

    /** A load planner takes over the identity of its account: the subject of the account is its id. */
    public static LoadPlannerId fromAccount(@Nullable String subject) {
        try {
            return new LoadPlannerId(UUID.fromString(String.valueOf(subject)));
        } catch (IllegalArgumentException e) {
            throw new UnknownLoadPlannerException(NOT_REGISTERED);
        }
    }

    public UnknownLoadPlannerException notRegistered() {
        return new UnknownLoadPlannerException(NOT_REGISTERED);
    }

    public NotFoundException notFound() {
        return new NotFoundException("Load planner [" + id + "] not found");
    }
}
