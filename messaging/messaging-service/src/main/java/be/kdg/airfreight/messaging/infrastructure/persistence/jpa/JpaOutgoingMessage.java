package be.kdg.airfreight.messaging.infrastructure.persistence.jpa;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import be.kdg.airfreight.messaging.domain.AirportCode;
import be.kdg.airfreight.messaging.domain.FlightId;
import be.kdg.airfreight.messaging.domain.FlightNumber;
import be.kdg.airfreight.messaging.domain.FormatVersion;
import be.kdg.airfreight.messaging.domain.MessageId;
import be.kdg.airfreight.messaging.domain.MessageType;
import be.kdg.airfreight.messaging.domain.outgoingmessage.OutgoingMessage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "outgoing_messages")
public class JpaOutgoingMessage {
    @Id
    private UUID id;

    @Column(nullable = false, length = 6)
    private String flightNumber;

    @Column(nullable = false)
    private LocalDate scheduledDepartureDate;

    @Column(nullable = false, length = 3)
    private String departureAirport;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MessageType type;

    @Column(nullable = false)
    private int formatVersion;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    private @Nullable Instant sentAt;

    protected JpaOutgoingMessage() {
    }

    public static JpaOutgoingMessage fromDomain(OutgoingMessage message) {
        JpaOutgoingMessage jpaMessage = new JpaOutgoingMessage();
        jpaMessage.id = message.getId().id();
        jpaMessage.flightNumber = message.getFlightId().flightNumber().value();
        jpaMessage.scheduledDepartureDate = message.getFlightId().scheduledDepartureDate();
        jpaMessage.departureAirport = message.getFlightId().departureAirport().value();
        jpaMessage.type = message.getType();
        jpaMessage.formatVersion = message.getFormatVersion().number();
        jpaMessage.createdAt = message.getCreatedAt();
        jpaMessage.body = message.getBody();
        jpaMessage.sentAt = message.getSentAt();
        return jpaMessage;
    }

    public OutgoingMessage toDomain() {
        return OutgoingMessage.hydrate(
            new MessageId(id),
            new FlightId(new FlightNumber(flightNumber), scheduledDepartureDate, new AirportCode(departureAirport)),
            type,
            new FormatVersion(formatVersion),
            createdAt,
            body,
            sentAt);
    }
}
