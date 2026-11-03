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
import be.kdg.airfreight.messaging.domain.receivedmessage.ReceivedMessage;
import be.kdg.airfreight.messaging.domain.receivedmessage.ReceivedMessageStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "received_messages")
public class JpaReceivedMessage {
    @Id
    private UUID id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MessageType type;

    private @Nullable Integer formatVersion;

    @Column(nullable = false)
    private Instant receivedAt;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ReceivedMessageStatus status;

    @Column(columnDefinition = "text")
    private @Nullable String rejectionReason;

    @Column(length = 7)
    private @Nullable String flightNumber;

    private @Nullable LocalDate scheduledDepartureDate;

    @Column(length = 3)
    private @Nullable String departureAirport;

    private @Nullable LocalDate derivedFlightDate;

    protected JpaReceivedMessage() {
    }

    public static JpaReceivedMessage fromDomain(ReceivedMessage message) {
        JpaReceivedMessage jpaMessage = new JpaReceivedMessage();
        jpaMessage.id = message.getId().id();
        jpaMessage.type = message.getType();
        FormatVersion version = message.getFormatVersion();
        jpaMessage.formatVersion = version == null ? null : version.number();
        jpaMessage.receivedAt = message.getReceivedAt();
        jpaMessage.body = message.getBody();
        jpaMessage.status = message.getStatus();
        jpaMessage.rejectionReason = message.getRejectionReason();
        FlightId flight = message.getFlightId();
        if (flight != null) {
            jpaMessage.flightNumber = flight.flightNumber().value();
            jpaMessage.scheduledDepartureDate = flight.scheduledDepartureDate();
            jpaMessage.departureAirport = flight.departureAirport().value();
        }
        jpaMessage.derivedFlightDate = message.getDerivedFlightDate();
        return jpaMessage;
    }

    public ReceivedMessage toDomain() {
        return ReceivedMessage.hydrate(
            new MessageId(id),
            type,
            formatVersion == null ? null : new FormatVersion(formatVersion),
            receivedAt,
            body,
            status,
            rejectionReason,
            flightId(),
            derivedFlightDate);
    }

    private @Nullable FlightId flightId() {
        if (flightNumber == null || scheduledDepartureDate == null || departureAirport == null) {
            return null;
        }
        return new FlightId(new FlightNumber(flightNumber), scheduledDepartureDate,
            new AirportCode(departureAirport));
    }
}
