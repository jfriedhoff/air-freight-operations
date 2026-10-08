package be.kdg.airfreight.simulator.acceptance.steps;

import java.time.Instant;
import java.time.LocalDate;

import be.kdg.airfreight.simulator.SentMessage;
import be.kdg.airfreight.simulator.StreamProbe;
import com.jayway.jsonpath.JsonPath;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.jspecify.annotations.Nullable;
import org.skyscreamer.jsonassert.JSONAssert;

import static org.assertj.core.api.Assertions.assertThat;

/** Story 32: booking records in, ULD messages out. */
public class BookingSteps {
    private final SimulatorWorld world;
    private final StreamProbe probe;
    /** Only these steps read it, so it is not in the World. */
    private @Nullable SentMessage uldMessage;

    public BookingSteps(SimulatorWorld world, StreamProbe probe) {
        this.world = world;
        this.probe = probe;
    }

    @When("I supply a booking record for {word} with scheduled departure date {date} from {word}")
    public void supplyABookingRecord(String flightNumber, LocalDate scheduledDepartureDate, String airport)
        throws Exception {
        world.post(SimulatorWorld.CARGO_BOOKINGS, """
            [{ "flightNumber": "%s", "scheduledDepartureDate": "%s", "departureAirport": "%s",
               "uldCode": "AKE16689R7", "weight": 1250.5, "weightUnit": "KG", "destination": "ATH" }]
            """.formatted(flightNumber, scheduledDepartureDate, airport));
    }

    /** A regular expression, because the flight number is followed by a comma. "09" is the day 9. */
    @Then("^the sent ULD message contains flight number (\\w+), day (\\d{2}) and airport (\\w+)$")
    public void theSentUldMessageContains(String flightNumber, String day, String airport) {
        uldMessage = probe.nextUldMessage();

        assertThat(JsonPath.<String>read(uldMessage.body(), "$.flightNumber")).isEqualTo(flightNumber);
        assertThat(JsonPath.<Integer>read(uldMessage.body(), "$.dayOfMonth")).isEqualTo(Integer.parseInt(day));
        assertThat(JsonPath.<String>read(uldMessage.body(), "$.departureAirport")).isEqualTo(airport);
    }

    /** Strict comparison: no scheduledDepartureDate in the message, only the day of the month. */
    @Then("it conforms to the fixed structure of a ULD message")
    public void itConformsToTheFixedStructure() throws Exception {
        SentMessage sent = assertThat(uldMessage).isNotNull().actual();
        String messageId = JsonPath.read(world.lastBody(), "$.messageIds[0]");
        String createdAt = JsonPath.read(sent.body(), "$.createdAt");

        assertThat(sent.routingKey()).isEqualTo("cargo-booking.uld");
        assertThat(sent.messageId()).isEqualTo(messageId);
        assertThat(Instant.parse(createdAt)).isBeforeOrEqualTo(Instant.now());
        JSONAssert.assertEquals("""
            { "messageId": "%s", "type": "cargo-booking.uld", "version": 1, "createdAt": "%s",
              "flightNumber": "HJ221", "dayOfMonth": 9, "departureAirport": "BRU", "uldCode": "AKE16689R7",
              "weight": 1250.5, "weightUnit": "KG", "destination": "ATH" }
            """.formatted(messageId, createdAt), sent.body(), true);
    }
}
