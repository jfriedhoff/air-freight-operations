package be.kdg.airfreight.flightoperations.infrastructure.flight.jpa;

import java.util.UUID;

import be.kdg.airfreight.flightoperations.domain.flight.Delay;
import be.kdg.airfreight.flightoperations.domain.flight.DelayCode;
import be.kdg.airfreight.flightoperations.domain.flight.DelayDuration;
import be.kdg.airfreight.flightoperations.domain.flight.DelayId;
import be.kdg.airfreight.flightoperations.domain.flight.DelayKind;
import be.kdg.airfreight.flightoperations.domain.flightplanner.FlightPlannerId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "delays")
public class JpaDelay {
    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "flight_id")
    private JpaFlight flight;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DelayKind kind;

    @Column(nullable = false)
    private int minutes;

    @Column(nullable = false, length = 2)
    private String reason;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private UUID lastChangedBy;

    protected JpaDelay() {
    }

    private JpaDelay(UUID id, JpaFlight flight, DelayKind kind, int minutes, String reason, String description,
                     UUID lastChangedBy) {
        this.id = id;
        this.flight = flight;
        this.kind = kind;
        this.minutes = minutes;
        this.reason = reason;
        this.description = description;
        this.lastChangedBy = lastChangedBy;
    }

    static JpaDelay fromDomain(Delay delay, JpaFlight flight) {
        return new JpaDelay(delay.getId().id(), flight, delay.getKind(), delay.getMinutes().minutes(),
            delay.getReason().value(), delay.getDescription(), delay.getLastChangedBy().id());
    }

    Delay toDomain() {
        return Delay.hydrate(new DelayId(id), kind, new DelayDuration(minutes), new DelayCode(reason),
            description, new FlightPlannerId(lastChangedBy));
    }
}
