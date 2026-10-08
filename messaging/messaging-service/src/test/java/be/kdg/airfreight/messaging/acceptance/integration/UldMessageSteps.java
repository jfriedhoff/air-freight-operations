package be.kdg.airfreight.messaging.acceptance.integration;

import be.kdg.airfreight.messaging.infrastructure.messaging.OutboxEventRepository;
import be.kdg.airfreight.messaging.infrastructure.persistence.jpa.JpaReceivedMessageRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.BDDMockito.given;

/**
 * Story 30 from the queue to the database: a ULD message is published on the cargo-booking exchange, as the
 * cargo booking system does, and UldMessageListener picks it up. The listener works asynchronously, so the
 * checks wait for the result. What Messaging sends on is checked in its outbox.
 */
public class UldMessageSteps {
    private static final String CARGO_BOOKING_MESSAGES = "cargo-booking.messages";
    private static final String ULD_ROUTING_KEY = "cargo-booking.uld";
    private static final String ULD_BOOKING_RECEIVED = "messaging.uld-booking.received";
    private static final String ULD_CODE = "AKE16689R7";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final RabbitTemplate rabbit;
    private final Clock clock;
    private final OutboxEventRepository outbox;
    private final JpaReceivedMessageRepository received;
    private final JsonMapper mapper;

    private final UUID messageId = UUID.randomUUID();
    private String flightNumber;
    private int dayOfMonth;
    private LocalDate firstFlight;

    public UldMessageSteps(RabbitTemplate rabbit, Clock clock, OutboxEventRepository outbox,
                           JpaReceivedMessageRepository received, JsonMapper mapper) {
        this.rabbit = rabbit;
        this.clock = clock;
        this.outbox = outbox;
        this.received = received;
        this.mapper = mapper;
    }

    @Given("a ULD message for {word}, day {day}, was processed on {date} and booked the ULD on the flight of {int} {month}")
    public void aUldMessageWasProcessed(String flightNumber, int dayOfMonth, LocalDate processedOn, int flightDay,
                                        Month flightMonth) {
        this.flightNumber = flightNumber;
        this.dayOfMonth = dayOfMonth;
        firstFlight = LocalDate.of(processedOn.getYear(), flightMonth, flightDay);
        itIs(processedOn);
        publish(messageId, ULD_CODE);
        await().atMost(TIMEOUT).untilAsserted(() -> assertThat(bookingsOn(firstFlight)).hasSize(1));
    }

    /**
     * After the duplicate a second, new message follows on the same queue. The queue has one consumer, so
     * messages are handled in order: once the second one is booked, the duplicate has been handled too.
     */
    @When("the same message arrives again on {date}")
    public void theSameMessageArrivesAgainOn(LocalDate date) {
        itIs(date);
        publish(messageId, ULD_CODE);
        String followUp = "PMC12345R7";
        publish(UUID.randomUUID(), followUp);
        await().atMost(TIMEOUT).untilAsserted(() -> assertThat(bookings())
            .anyMatch(booking -> booking.uldCode().equals(followUp)));
    }

    @Then("it is recognized as the same message")
    public void itIsRecognizedAsTheSameMessage() {
        assertThat(received.findById(messageId)).get()
            .extracting(message -> message.toDomain().getDerivedFlightDate())
            .isEqualTo(firstFlight);
        assertThat(bookings()).filteredOn(booking -> booking.uldCode().equals(ULD_CODE)).hasSize(1);
    }

    @Then("the ULD is not booked on the flight of {int} {month}")
    public void theUldIsNotBookedOnTheFlightOf(int day, Month month) {
        assertThat(bookingsOn(LocalDate.of(firstFlight.getYear(), month, day))).isEmpty();
    }

    private void itIs(LocalDate date) {
        given(clock.instant()).willReturn(date.atTime(12, 0).toInstant(ZoneOffset.UTC));
    }

    private void publish(UUID id, String uldCode) {
        String body = """
            {"messageId":"%s","type":"cargo-booking.uld","version":1,"createdAt":"%s",\
            "flightNumber":"%s","dayOfMonth":%d,"departureAirport":"BRU",\
            "uldCode":"%s","weight":1250,"weightUnit":"KG","destination":"ATH"}"""
            .formatted(id, clock.instant(), flightNumber, dayOfMonth, uldCode);
        rabbit.send(CARGO_BOOKING_MESSAGES, ULD_ROUTING_KEY, MessageBuilder
            .withBody(body.getBytes(StandardCharsets.UTF_8))
            .setContentType("application/json")
            .build());
    }

    private List<Booking> bookingsOn(LocalDate flight) {
        return bookings().stream()
            .filter(booking -> booking.uldCode().equals(ULD_CODE) && booking.scheduledDepartureDate().equals(flight))
            .toList();
    }

    private List<Booking> bookings() {
        return outbox.findAll().stream()
            .filter(event -> event.getType().equals(ULD_BOOKING_RECEIVED))
            .map(event -> mapper.readValue(event.getBody(), Booking.class))
            .toList();
    }

    /** The part of the uld-booking.received event that this story is about. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Booking(String uldCode, LocalDate scheduledDepartureDate) {
    }
}
