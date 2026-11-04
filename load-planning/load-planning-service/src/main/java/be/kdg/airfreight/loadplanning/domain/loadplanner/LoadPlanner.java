package be.kdg.airfreight.loadplanning.domain.loadplanner;

import java.util.Objects;

import be.kdg.airfreight.loadplanning.domain.InvalidValueException;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.jspecify.annotations.Nullable;

@AggregateRoot
public class LoadPlanner {
    @Identity
    private final LoadPlannerId id;
    private final String name;

    public LoadPlanner(LoadPlannerId id, String name) {
        this.id = Objects.requireNonNull(id, "Id cannot be null");
        Objects.requireNonNull(name, "Name cannot be null");
        if (name.isBlank()) {
            throw new InvalidValueException("The name of a load planner cannot be blank");
        }
        this.name = name;
    }

    public LoadPlannerId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        return this == other || other instanceof LoadPlanner planner && id.equals(planner.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
