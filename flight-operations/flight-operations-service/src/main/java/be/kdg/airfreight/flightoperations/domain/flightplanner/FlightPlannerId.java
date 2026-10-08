package be.kdg.airfreight.flightoperations.domain.flightplanner;

import java.util.Objects;
import java.util.UUID;

import be.kdg.airfreight.flightoperations.domain.NotFoundException;
import org.jmolecules.ddd.annotation.ValueObject;
import org.jspecify.annotations.Nullable;

@ValueObject
public record FlightPlannerId(UUID id) {
    private static final String NOT_REGISTERED = "You are not registered as a flight planner";

    public FlightPlannerId {
        Objects.requireNonNull(id, "Id cannot be null");
    }

    public static FlightPlannerId create() {
        return new FlightPlannerId(UUID.randomUUID());
    }

    /** A flight planner takes over the identity of its account: the subject of the account is its id. */
    public static FlightPlannerId fromAccount(@Nullable String subject) {
        try {
            return new FlightPlannerId(UUID.fromString(String.valueOf(subject)));
        } catch (IllegalArgumentException e) {
            throw new UnknownFlightPlannerException(NOT_REGISTERED);
        }
    }

    public UnknownFlightPlannerException notRegistered() {
        return new UnknownFlightPlannerException(NOT_REGISTERED);
    }

    public NotFoundException notFound() {
        return new NotFoundException("Flight planner [" + id + "] not found");
    }
}
