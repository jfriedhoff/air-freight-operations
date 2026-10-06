package be.kdg.airfreight.loadplanning.domain.flightcargo;

import java.util.Objects;

import be.kdg.airfreight.loadplanning.domain.loadplanner.LoadPlannerId;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;
import org.jspecify.annotations.Nullable;

@Entity
public class Uld {
    @Identity
    private final UldCode code;
    private final Weight weight;
    private final AirportCode destination;
    private @Nullable LoadPosition position;
    private @Nullable LoadPlannerId positionChangedBy;

    Uld(UldCode code, Weight weight, AirportCode destination) {
        this.code = Objects.requireNonNull(code, "Code cannot be null");
        this.weight = Objects.requireNonNull(weight, "Weight cannot be null");
        this.destination = Objects.requireNonNull(destination, "Destination cannot be null");
    }

    public static Uld hydrate(UldCode code, Weight weight, AirportCode destination,
                              @Nullable LoadPosition position, @Nullable LoadPlannerId positionChangedBy) {
        Uld uld = new Uld(code, weight, destination);
        uld.position = position;
        uld.positionChangedBy = positionChangedBy;
        return uld;
    }

    void assign(LoadPosition position, LoadPlannerId by) {
        this.position = Objects.requireNonNull(position, "Position cannot be null");
        this.positionChangedBy = Objects.requireNonNull(by, "Load planner cannot be null");
    }

    void clear(LoadPlannerId by) {
        this.position = null;
        this.positionChangedBy = Objects.requireNonNull(by, "Load planner cannot be null");
    }

    public boolean hasPosition() {
        return position != null;
    }

    public boolean isAt(LoadPosition position) {
        return position.equals(this.position);
    }

    public boolean isOnSide(Side side) {
        return position != null && position.side() == side;
    }

    public UldCode getCode() {
        return code;
    }

    public Weight getWeight() {
        return weight;
    }

    public AirportCode getDestination() {
        return destination;
    }

    public @Nullable LoadPosition getPosition() {
        return position;
    }

    public @Nullable LoadPlannerId getPositionChangedBy() {
        return positionChangedBy;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        return this == other || other instanceof Uld uld && code.equals(uld.code);
    }

    @Override
    public int hashCode() {
        return code.hashCode();
    }
}
