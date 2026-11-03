package be.kdg.airfreight.flightoperations.infrastructure.messaging;

import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.util.UUID;

import be.kdg.airfreight.flightoperations.application.FinalizedLoadService;
import be.kdg.airfreight.flightoperations.application.FlightSchedulingService;
import be.kdg.airfreight.flightoperations.application.command.RecordFinalizedLoadCommand;
import be.kdg.airfreight.flightoperations.application.command.ScheduleFlightCommand;
import be.kdg.airfreight.flightoperations.domain.DomainRuleViolationException;
import be.kdg.airfreight.flightoperations.domain.NotFoundException;
import be.kdg.airfreight.flightoperations.domain.flight.AircraftRegistration;
import be.kdg.airfreight.flightoperations.domain.flight.AirportCode;
import be.kdg.airfreight.flightoperations.domain.flight.FlightNumber;
import be.kdg.airfreight.flightoperations.domain.flight.ScheduledTimes;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Inbound adapter: a controller, but for messages. Reads what Messaging tells this context, translates
 * it into a command and calls the use case. A message that can never work is rejected at once and ends
 * on the dead letter queue; anything unexpected is left to the retries. A missing field is checked here, by
 * name, so that a NullPointerException from a bug is not mistaken for an invalid message.
 */
@Slf4j
@Component
class MessagingEventListener {
    /** The version of messaging.flight.scheduled and messaging.cpm.created that this listener understands. */
    private static final int VERSION = 1;

    private final JsonMapper mapper;
    private final FlightSchedulingService scheduling;
    private final FinalizedLoadService finalizedLoads;
    private final AmqpProperties amqp;

    MessagingEventListener(JsonMapper mapper, FlightSchedulingService scheduling,
                           FinalizedLoadService finalizedLoads, AmqpProperties amqp) {
        this.mapper = mapper;
        this.scheduling = scheduling;
        this.finalizedLoads = finalizedLoads;
        this.amqp = amqp;
    }

    @RabbitListener(queues = "${flight-operations.amqp.queue}")
    void on(Message message) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        UUID messageId = null;
        try {
            Envelope envelope = mapper.readValue(body, Envelope.class);
            messageId = envelope.messageId();
            requireKnownVersion(envelope);
            switch (String.valueOf(envelope.type())) {
                case MessagingConfig.FLIGHT_SCHEDULED ->
                    scheduling.scheduleFlight(toCommand(mapper.readValue(body, FlightScheduledJson.class)));
                case MessagingConfig.CPM_CREATED ->
                    finalizedLoads.recordFinalizedLoad(toCommand(mapper.readValue(body, CpmCreatedJson.class)));
                default -> throw new DomainRuleViolationException("Unknown message type '" + envelope.type() + "'");
            }
        } catch (JacksonException | DomainRuleViolationException | NotFoundException | DateTimeException e) {
            String reason = e.getMessage() == null ? "a required field is missing"
                : e.getMessage().lines().findFirst().orElse("");
            log.warn("Message {} rejected and set aside on {}: {}", messageId, amqp.deadLetterQueue(), reason);
            throw new AmqpRejectAndDontRequeueException("Message " + messageId + " rejected: " + reason, e);
        }
    }

    /** A newer version of a contract may mean something else with the same fields: it is refused, not guessed. */
    private static void requireKnownVersion(Envelope envelope) {
        int version = required(envelope.version(), "version");
        if (version != VERSION) {
            throw new DomainRuleViolationException("Version " + version + " of '" + envelope.type()
                + "' is unknown: expected version " + VERSION);
        }
    }

    private static ScheduleFlightCommand toCommand(FlightScheduledJson json) {
        return new ScheduleFlightCommand(new FlightNumber(required(json.flightNumber(), "flightNumber")),
            new AirportCode(required(json.departureAirport(), "departureAirport")),
            new AirportCode(required(json.arrivalAirport(), "arrivalAirport")),
            new AircraftRegistration(required(json.registration(), "registration")),
            new ScheduledTimes(required(json.std(), "std"), required(json.sta(), "sta")));
    }

    private static RecordFinalizedLoadCommand toCommand(CpmCreatedJson json) {
        return new RecordFinalizedLoadCommand(new FlightNumber(required(json.flightNumber(), "flightNumber")),
            required(json.scheduledDepartureDate(), "scheduledDepartureDate"),
            new AirportCode(required(json.departureAirport(), "departureAirport")));
    }

    private static <V> V required(@Nullable V value, String field) {
        if (value == null) {
            throw new DomainRuleViolationException("Field '" + field + "' is missing");
        }
        return value;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Envelope(@Nullable UUID messageId, @Nullable String type, @Nullable Integer version) {
    }
}
