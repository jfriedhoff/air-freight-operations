package be.kdg.airfreight.flightoperations.domain.flight;

import java.util.Objects;

import be.kdg.airfreight.flightoperations.domain.InvalidValueException;
import be.kdg.airfreight.flightoperations.domain.flightplanner.FlightPlannerId;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;
import org.jspecify.annotations.Nullable;

@Entity
public class Delay {
    /** JpaDelay sizes the description column with this value, so a description the domain accepts always fits. */
    public static final int MAX_DESCRIPTION_LENGTH = 255;

    @Identity
    private final DelayId id;
    private final DelayKind kind;
    private final DelayCode reason;
    private DelayDuration minutes;
    private String description;
    private FlightPlannerId lastChangedBy;

    Delay(DelayId id, DelayKind kind, DelayDuration minutes, DelayCode reason, String description,
          FlightPlannerId lastChangedBy) {
        this.id = Objects.requireNonNull(id, "Id cannot be null");
        this.kind = Objects.requireNonNull(kind, "Kind cannot be null");
        this.reason = Objects.requireNonNull(reason, "Reason cannot be null");
        this.minutes = Objects.requireNonNull(minutes, "Minutes cannot be null");
        this.description = requireDescription(description);
        this.lastChangedBy = Objects.requireNonNull(lastChangedBy, "Flight planner cannot be null");
    }

    public static Delay hydrate(DelayId id, DelayKind kind, DelayDuration minutes, DelayCode reason,
                                String description, FlightPlannerId lastChangedBy) {
        return new Delay(id, kind, minutes, reason, description, lastChangedBy);
    }

    void change(DelayDuration minutes, String description, FlightPlannerId by) {
        // checked before anything is assigned, so a refused change leaves the delay as it was
        String checkedDescription = requireDescription(description);
        this.minutes = Objects.requireNonNull(minutes, "Minutes cannot be null");
        this.description = checkedDescription;
        this.lastChangedBy = Objects.requireNonNull(by, "Flight planner cannot be null");
    }

    public DelayId getId() {
        return id;
    }

    public DelayKind getKind() {
        return kind;
    }

    public DelayDuration getMinutes() {
        return minutes;
    }

    public DelayCode getReason() {
        return reason;
    }

    public String getDescription() {
        return description;
    }

    public FlightPlannerId getLastChangedBy() {
        return lastChangedBy;
    }

    private static String requireDescription(String description) {
        Objects.requireNonNull(description, "Description cannot be null");
        if (description.isBlank()) {
            throw new InvalidValueException("The description of a delay cannot be blank");
        }
        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new InvalidValueException(
                "The description of a delay is at most " + MAX_DESCRIPTION_LENGTH + " characters long");
        }
        return description;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        return this == other || other instanceof Delay delay && id.equals(delay.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
