package be.kdg.airfreight.loadplanning.infrastructure.flightcargo.jpa;

import java.math.BigDecimal;
import java.util.UUID;

import be.kdg.airfreight.loadplanning.domain.flightcargo.AirportCode;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadPosition;
import be.kdg.airfreight.loadplanning.domain.flightcargo.Side;
import be.kdg.airfreight.loadplanning.domain.flightcargo.Uld;
import be.kdg.airfreight.loadplanning.domain.flightcargo.UldCode;
import be.kdg.airfreight.loadplanning.domain.flightcargo.Weight;
import be.kdg.airfreight.loadplanning.domain.flightcargo.WeightUnit;
import be.kdg.airfreight.loadplanning.domain.loadplanner.LoadPlannerId;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "ulds")
public class JpaUld {
    @EmbeddedId
    private JpaUldId id;

    @MapsId("flightCargoId")
    @ManyToOne(optional = false)
    @JoinColumn(name = "flight_cargo_id")
    private JpaFlightCargo flightCargo;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal weightValue;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private WeightUnit weightUnit;

    @Column(nullable = false, length = 3)
    private String destination;

    private @Nullable String positionName;

    @Enumerated(EnumType.STRING)
    private @Nullable Side positionSide;

    private @Nullable UUID positionChangedBy;

    protected JpaUld() {
    }

    static JpaUld fromDomain(Uld uld, JpaFlightCargo flightCargo) {
        JpaUld jpaUld = new JpaUld();
        jpaUld.id = new JpaUldId(flightCargo.getId(), uld.getCode().value());
        jpaUld.flightCargo = flightCargo;
        jpaUld.weightValue = uld.getWeight().value();
        jpaUld.weightUnit = uld.getWeight().unit();
        jpaUld.destination = uld.getDestination().value();
        LoadPosition position = uld.getPosition();
        jpaUld.positionName = position == null ? null : position.name();
        jpaUld.positionSide = position == null ? null : position.side();
        LoadPlannerId changedBy = uld.getPositionChangedBy();
        jpaUld.positionChangedBy = changedBy == null ? null : changedBy.id();
        return jpaUld;
    }

    Uld toDomain() {
        return Uld.hydrate(
            new UldCode(id.getCode()),
            new Weight(weightValue, weightUnit),
            new AirportCode(destination),
            positionName == null ? null : new LoadPosition(positionName, positionSide),
            positionChangedBy == null ? null : new LoadPlannerId(positionChangedBy));
    }
}
