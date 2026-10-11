package be.kdg.airfreight.loadplanning.acceptance.integration;

import be.kdg.airfreight.loadplanning.domain.flightcargo.AirportCode;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargoRepository;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightId;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightNumber;
import be.kdg.airfreight.loadplanning.domain.flightcargo.UldCode;
import be.kdg.airfreight.loadplanning.domain.flightcargo.Weight;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.awaitility.Awaitility.await;

/**
 * Plays Messaging: sends its events on its exchange, as JSON in its envelope, so they reach Load Planning
 * through RabbitMQ and MessagingEventListener as in production. Every message gets a new identification.
 */
@Component
public class MessagingEvents {
    private static final String MESSAGING_EVENTS = "messaging.events";
    private static final String DEAD_LETTER_QUEUE = "load-planning.dlq";
    /** A refused message is retried three times (1 + 2 + 4 s) before it is set aside. */
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final LocalTime STD = LocalTime.of(6, 45);
    private static final LocalTime STA = LocalTime.of(10, 0);
    private static final LocalDate FOLLOW_UP_DATE = LocalDate.of(2026, 1, 1);

    private final RabbitTemplate rabbit;
    private final JsonMapper mapper;
    private final FlightCargoRepository cargos;
    private final AtomicInteger followUps = new AtomicInteger();

    public MessagingEvents(RabbitTemplate rabbit, JsonMapper mapper, FlightCargoRepository cargos) {
        this.rabbit = rabbit;
        this.mapper = mapper;
        this.cargos = cargos;
    }

    public void flightAnnounced(FlightId flight) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("arrivalAirport", "ATH");
        fields.put("std", utc(flight.scheduledDepartureDate(), STD));
        fields.put("sta", utc(flight.scheduledDepartureDate(), STA));
        send("messaging.flight.announced", flight, fields);
    }

    public void flightDeparted(FlightId flight) {
        send("messaging.flight.departed", flight, Map.of());
    }

    public void departureEstimateChanged(FlightId flight, Instant etd) {
        send("messaging.flight.departure-estimate-changed", flight, Map.of("etd", etd.toString()));
    }

    /** Messaging has already turned the day of the month into a date: Load Planning gets that date. */
    public UUID uldBookingReceived(FlightId flight, UldCode code, Weight weight) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("uldCode", code.value());
        fields.put("weight", weight.value());
        fields.put("weightUnit", weight.unit().name());
        fields.put("destination", "ATH");
        return send("messaging.uld-booking.received", flight, fields);
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
            Message letter = rabbit.receive(DEAD_LETTER_QUEUE, 500);
            return letter != null
                && new String(letter.getBody(), StandardCharsets.UTF_8).contains(messageId.toString());
        });
    }

    private UUID send(String type, FlightId flight, Map<String, Object> fields) {
        UUID messageId = UUID.randomUUID();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("messageId", messageId.toString());
        body.put("type", type);
        body.put("version", 1);
        body.put("occurredOn", Instant.now().toString());
        body.put("flightNumber", flight.flightNumber().value());
        body.put("scheduledDepartureDate", flight.scheduledDepartureDate().toString());
        body.put("departureAirport", flight.departureAirport().value());
        body.putAll(fields);
        rabbit.send(MESSAGING_EVENTS, type, MessageBuilder
            .withBody(mapper.writeValueAsString(body).getBytes(StandardCharsets.UTF_8))
            .setContentType("application/json")
            .build());
        return messageId;
    }

    private static String utc(LocalDate date, LocalTime time) {
        return date.atTime(time).toInstant(ZoneOffset.UTC).toString();
    }
}
