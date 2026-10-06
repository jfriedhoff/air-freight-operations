package be.kdg.airfreight.loadplanning.infrastructure.flightcargo.jpa;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.jspecify.annotations.Nullable;

@Embeddable
public class JpaUldId implements Serializable {
    @Column(name = "flight_cargo_id")
    private UUID flightCargoId;

    @Column(name = "code", length = 10)
    private String code;

    protected JpaUldId() {
    }

    JpaUldId(UUID flightCargoId, String code) {
        this.flightCargoId = flightCargoId;
        this.code = code;
    }

    String getCode() {
        return code;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        return this == other || other instanceof JpaUldId id
            && Objects.equals(flightCargoId, id.flightCargoId) && Objects.equals(code, id.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(flightCargoId, code);
    }
}
