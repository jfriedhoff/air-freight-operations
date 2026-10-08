package be.kdg.airfreight.flightoperations.domain.flightplanner;

import java.util.Objects;

import be.kdg.airfreight.flightoperations.domain.InvalidValueException;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.jspecify.annotations.Nullable;

@AggregateRoot
public class FlightPlanner {
    @Identity
    private final FlightPlannerId id;
    private final String name;

    public FlightPlanner(FlightPlannerId id, String name) {
        this.id = Objects.requireNonNull(id, "Id cannot be null");
        Objects.requireNonNull(name, "Name cannot be null");
        if (name.isBlank()) {
            throw new InvalidValueException("The name of a flight planner cannot be blank");
        }
        this.name = name;
    }

    public FlightPlannerId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        return this == other || other instanceof FlightPlanner planner && id.equals(planner.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
