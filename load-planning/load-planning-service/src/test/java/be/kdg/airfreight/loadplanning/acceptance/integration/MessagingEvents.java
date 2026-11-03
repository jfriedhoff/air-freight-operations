package be.kdg.airfreight.loadplanning.acceptance.integration;

import be.kdg.airfreight.loadplanning.domain.flightcargo.AirportCode;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargoRepository;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightId;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightNumber;
import be.kdg.airfreight.loadplanning.domain.flightcargo.UldCode;
import be.kdg.airfreight.loadplanning.domain.flightcargo.Weight;
import be.kdg.airfreight.loadplanning.infrastructure.messaging.AmqpProperties;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.awaitility.Awaitility.await;

/**
 * Plays Messaging: sends its events on its exchange, as JSON in its envelope, so they reach Load Planning
 * through RabbitMQ and MessagingEventListener as in production. Every message gets a new identification,
 * except one that is sent again with sendAgain.
 */
@Component
public class MessagingEvents {
    /** A refused message is retried three times (1 + 2 + 4 s) before it is set aside. */
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final LocalTime STD = LocalTime.of(6, 45);
    private static final LocalTime STA = LocalTime.of(10, 0);
    private static final LocalDate FOLLOW_UP_DATE = LocalDate.of(2026, 1, 1);

    private final RabbitTemplate rabbit;
    private final JsonMapper mapper;
    private final FlightCargoRepository cargos;
    private final AmqpProperties amqp;
    private final Clock clock;
    private final AtomicInteger followUps = new AtomicInteger();
    /** Every message sent so far, by its identification, so that a scenario can send one of them again. */
    private final Map<UUID, Sent> sent = new ConcurrentHashMap<>();

    public MessagingEvents(RabbitTemplate rabbit, JsonMapper mapper, FlightCargoRepository cargos,
                           AmqpProperties amqp, Clock clock) {
        this.rabbit = rabbit;
        this.mapper = mapper;
        this.cargos = cargos;
        this.amqp = amqp;
        this.clock = clock;
    }

    public UUID flightAnnounced(FlightId flight) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("arrivalAirport", "ATH");
        fields.put("std", utc(flight.scheduledDepartureDate(), STD));
        fields.put("sta", utc(flight.scheduledDepartureDate(), STA));
        return send("messaging.flight.announced", flight, fields);
    }

    public void flightDeparted(FlightId flight) {
        send("messaging.flight.departed", flight, Map.of());
    }

    public UUID departureEstimateChanged(FlightId flight, Instant etd) {
        return departureEstimateChanged(flight, etd, clock.instant());
    }

    /** A new ETD that was entered in Flight Operations at occurredOn, which may be long before it arrives. */
    public UUID departureEstimateChanged(FlightId flight, Instant etd, Instant occurredOn) {
        return send("messaging.flight.departure-estimate-changed", flight, Map.of("etd", etd.toString()), occurredOn);
    }

    /** A new ETD that is not a moment: Load Planning cannot read the message. */
    public UUID departureEstimateChangedWithoutAMoment(FlightId flight) {
        return send("messaging.flight.departure-estimate-changed", flight, Map.of("etd", "soon"));
    }

    /** Messaging has already turned the day of the month into a date: Load Planning gets that date. */
    public UUID uldBookingReceived(FlightId flight, UldCode code, Weight weight) {
        return uldBookingReceived(flight, code, weight, body -> { });
    }

    /**
     * A booking as Messaging would never send it: change removes or replaces a field of the message, so that Load
     * Planning has to refuse it.
     */
    public UUID uldBookingReceived(FlightId flight, UldCode code, Weight weight, Consumer<Map<String, Object>> change) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("uldCode", code.value());
        fields.put("weight", weight.value());
        fields.put("weightUnit", weight.unit().name());
        fields.put("destination", "ATH");
        return send("messaging.uld-booking.received", flight, fields, clock.instant(), change);
    }

    /**
     * Sends a message again with the same identification and the same content, as RabbitMQ does when it
     * delivers a message twice, or as an outbox does when it publishes a row again after a crash.
     */
    public void sendAgain(UUID messageId) {
        Sent message = sent.get(messageId);
        rabbit.send(amqp.messagingEventsExchange(), message.type(), message.message());
    }

    /**
     * Returns once every message sent so far has been handled. The queue has one consumer, so messages are
     * handled in the order they arrive: once a flight announced after them exists, the earlier ones are done.
     */
    public void waitUntilHandled() {
        FlightId followUp = new FlightId(new FlightNumber("ZZ" + followUps.incrementAndGet()), FOLLOW_UP_DATE,
            new AirportCode("BRU"));
        flightAnnounced(followUp);
        await().atMost(TIMEOUT).until(() -> cargos.findByFlightId(followUp).isPresent());
    }

    /** Takes dead letters until the one with this identification; older ones (from earlier runs) are dropped. */
    public void assertSetAside(UUID messageId) {
        await().atMost(TIMEOUT).until(() -> {
            Message letter = rabbit.receive(amqp.deadLetterQueue(), 500);
            return letter != null
                && new String(letter.getBody(), StandardCharsets.UTF_8).contains(messageId.toString());
        });
    }

    /** An event that happened in Flight Operations at the time of the test clock. */
    private UUID send(String type, FlightId flight, Map<String, Object> fields) {
        return send(type, flight, fields, clock.instant());
    }

    private UUID send(String type, FlightId flight, Map<String, Object> fields, Instant occurredOn) {
        return send(type, flight, fields, occurredOn, body -> { });
    }

    private UUID send(String type, FlightId flight, Map<String, Object> fields, Instant occurredOn,
                      Consumer<Map<String, Object>> change) {
        UUID messageId = UUID.randomUUID();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("messageId", messageId.toString());
        body.put("type", type);
        body.put("version", 1);
        body.put("occurredOn", occurredOn.toString());
        body.put("flightNumber", flight.flightNumber().value());
        body.put("scheduledDepartureDate", flight.scheduledDepartureDate().toString());
        body.put("departureAirport", flight.departureAirport().value());
        body.putAll(fields);
        change.accept(body);
        Message message = MessageBuilder
            .withBody(mapper.writeValueAsString(body).getBytes(StandardCharsets.UTF_8))
            .setContentType("application/json")
            .setMessageId(messageId.toString())
            .build();
        sent.put(messageId, new Sent(type, message));
        rabbit.send(amqp.messagingEventsExchange(), type, message);
        return messageId;
    }

    /** A message as it was sent, with its type, which is also its routing key. */
    private record Sent(String type, Message message) {
    }

    private static String utc(LocalDate date, LocalTime time) {
        return date.atTime(time).toInstant(ZoneOffset.UTC).toString();
    }
}
