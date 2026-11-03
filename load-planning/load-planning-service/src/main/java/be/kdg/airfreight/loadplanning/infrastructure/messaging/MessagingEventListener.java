package be.kdg.airfreight.loadplanning.infrastructure.messaging;

import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.util.UUID;

import be.kdg.airfreight.loadplanning.application.BookingService;
import be.kdg.airfreight.loadplanning.application.FlightInfoService;
import be.kdg.airfreight.loadplanning.application.command.AnnounceFlightCommand;
import be.kdg.airfreight.loadplanning.application.command.BookUldCommand;
import be.kdg.airfreight.loadplanning.domain.DomainRuleViolationException;
import be.kdg.airfreight.loadplanning.domain.NotFoundException;
import be.kdg.airfreight.loadplanning.domain.flightcargo.AirportCode;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightId;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightNumber;
import be.kdg.airfreight.loadplanning.domain.flightcargo.UldCode;
import be.kdg.airfreight.loadplanning.domain.flightcargo.Weight;
import be.kdg.airfreight.loadplanning.domain.flightcargo.WeightUnit;
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
 * it into this context's own terms and calls the use case. A message that can never work is rejected at
 * once and ends on the dead letter queue; anything unexpected is left to the retries.
 */
@Slf4j
@Component
class MessagingEventListener {
    private final JsonMapper mapper;
    private final FlightInfoService flightInfo;
    private final BookingService bookings;
    private final AmqpProperties amqp;

    MessagingEventListener(JsonMapper mapper, FlightInfoService flightInfo, BookingService bookings,
                           AmqpProperties amqp) {
        this.mapper = mapper;
        this.flightInfo = flightInfo;
        this.bookings = bookings;
        this.amqp = amqp;
    }

    @RabbitListener(queues = "${load-planning.amqp.queue}")
    void on(Message message) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        UUID messageId = null;
        try {
            Envelope envelope = mapper.readValue(body, Envelope.class);
            messageId = envelope.messageId();
            String type = String.valueOf(envelope.type());
            if (type.equals(MessagingConfig.ULD_BOOKING_RECEIVED)) {
                bookings.bookUld(toCommand(mapper.readValue(body, UldBookingReceivedJson.class)));
            } else {
                handleFlightEvent(type, mapper.readValue(body, FlightEventJson.class));
            }
        } catch (JacksonException | DomainRuleViolationException | NotFoundException | NullPointerException
                 | DateTimeException | IllegalArgumentException e) {
            String reason = e.getMessage() == null ? "a required field is missing"
                : e.getMessage().lines().findFirst().orElse("");
            log.warn("Message {} rejected and set aside on {}: {}", messageId, amqp.deadLetterQueue(), reason);
            throw new AmqpRejectAndDontRequeueException("Message " + messageId + " rejected: " + reason, e);
        }
    }

    private void handleFlightEvent(String type, FlightEventJson json) {
        FlightId flightId = new FlightId(new FlightNumber(json.flightNumber()),
            required(json.scheduledDepartureDate(), "scheduledDepartureDate"),
            new AirportCode(json.departureAirport()));
        switch (type) {
            case MessagingConfig.FLIGHT_ANNOUNCED -> flightInfo.flightAnnounced(new AnnounceFlightCommand(flightId,
                new AirportCode(required(json.arrivalAirport(), "arrivalAirport")), required(json.std(), "std"),
                required(json.sta(), "sta")));
            case MessagingConfig.DEPARTURE_ESTIMATE_CHANGED ->
                flightInfo.departureEstimateChanged(flightId, required(json.etd(), "etd"),
                    required(json.occurredOn(), "occurredOn"));
            case MessagingConfig.ARRIVAL_ESTIMATE_CHANGED ->
                flightInfo.arrivalEstimateChanged(flightId, required(json.eta(), "eta"));
            case MessagingConfig.FLIGHT_DEPARTED -> flightInfo.flightDeparted(flightId);
            case MessagingConfig.FLIGHT_LANDED -> log.debug("Flight {} of {} landed: nothing changes for its cargo",
                json.flightNumber(), json.scheduledDepartureDate());
            case MessagingConfig.FLIGHT_REMOVED -> flightInfo.flightRemoved(flightId);
            default -> throw new DomainRuleViolationException("Unknown message type '" + type + "'");
        }
    }

    /** An unknown weight unit makes WeightUnit.valueOf throw IllegalArgumentException: rejected like the rest. */
    private static BookUldCommand toCommand(UldBookingReceivedJson json) {
        return new BookUldCommand(
            new FlightId(new FlightNumber(json.flightNumber()),
                required(json.scheduledDepartureDate(), "scheduledDepartureDate"),
                new AirportCode(json.departureAirport())),
            new UldCode(json.uldCode()),
            new Weight(json.weight(), WeightUnit.valueOf(required(json.weightUnit(), "weightUnit"))),
            new AirportCode(json.destination()));
    }

    private static <V> V required(@Nullable V value, String field) {
        if (value == null) {
            throw new DomainRuleViolationException("Field '" + field + "' is missing");
        }
        return value;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Envelope(@Nullable UUID messageId, @Nullable String type) {
    }
}
