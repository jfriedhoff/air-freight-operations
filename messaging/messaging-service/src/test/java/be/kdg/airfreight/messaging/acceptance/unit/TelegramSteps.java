package be.kdg.airfreight.messaging.acceptance.unit;

import be.kdg.airfreight.messaging.domain.AircraftRegistration;
import be.kdg.airfreight.messaging.domain.AirportCode;
import be.kdg.airfreight.messaging.domain.FlightId;
import be.kdg.airfreight.messaging.domain.FlightNumber;
import be.kdg.airfreight.messaging.domain.FormatVersion;
import be.kdg.airfreight.messaging.domain.MessageId;
import be.kdg.airfreight.messaging.domain.flight.ArrivalEstimateChanged;
import be.kdg.airfreight.messaging.domain.flight.DepartureEstimateChanged;
import be.kdg.airfreight.messaging.domain.flight.FlightDeparted;
import be.kdg.airfreight.messaging.domain.flight.FlightEvent;
import be.kdg.airfreight.messaging.domain.flight.FlightLanded;
import be.kdg.airfreight.messaging.domain.outgoingmessage.MessageWriter;
import be.kdg.airfreight.messaging.domain.telegram.AaWriter;
import be.kdg.airfreight.messaging.domain.telegram.AdWriter;
import be.kdg.airfreight.messaging.domain.telegram.EaWriter;
import be.kdg.airfreight.messaging.domain.telegram.EdWriter;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Stories 11 and 14. "Flight Operations reports ..." builds the flight events as JsonFlightEventReader reads them
 * and gives each event to the MVT writers, as FlightEventService does: every writer is asked whether it writes
 * the event. Reading the event from the queue and sending the telegram are tested by the integration tests.
 */
public class TelegramSteps {
    private static final FormatVersion VERSION = new FormatVersion(1);
    private static final Instant REPORTED_AT = Instant.parse("2026-01-08T12:00:00Z");
    private static final List<MessageWriter<FlightEvent>> WRITERS =
        List.of(new EdWriter(), new EaWriter(), new AdWriter(), new AaWriter());

    private final List<String> telegrams = new ArrayList<>();
    private FlightId flight;
    private AircraftRegistration registration;
    private AirportCode arrivalAirport;

    @Given("flight {word} of {date} with registration {word} from {word} to {word}")
    public void flight(String flightNumber, LocalDate date, String registration, String from, String to) {
        flight = new FlightId(new FlightNumber(flightNumber), date, new AirportCode(from));
        this.registration = new AircraftRegistration(registration);
        arrivalAirport = new AirportCode(to);
    }

    /**
     * The background of story 11 names the scheduled times for its @e2e scenarios. An MVT does not contain them,
     * so Messaging needs nothing from this step.
     */
    @Given("the STD is {time} and the STA {time} UTC")
    public void theScheduledTimes(LocalTime std, LocalTime sta) {
    }

    @When("Flight Operations reports an ETD of {moment} and an ETA of {moment} UTC")
    public void flightOperationsReportsNewEstimates(Instant etd, Instant eta) {
        write(new DepartureEstimateChanged(MessageId.create(), VERSION, flight, UUID.randomUUID(), registration,
            etd, REPORTED_AT));
        write(new ArrivalEstimateChanged(MessageId.create(), VERSION, flight, UUID.randomUUID(), arrivalAirport,
            registration, eta, REPORTED_AT));
    }

    @When("Flight Operations reports an ATD of {moment} and an ATA of {moment} UTC")
    public void flightOperationsReportsActualTimes(Instant atd, Instant ata) {
        write(new FlightDeparted(MessageId.create(), VERSION, flight, UUID.randomUUID(), registration, atd,
            REPORTED_AT));
        write(new FlightLanded(MessageId.create(), VERSION, flight, UUID.randomUUID(), arrivalAirport,
            registration, ata, REPORTED_AT));
    }

    @Then("this telegram is written:")
    public void thisTelegramIsWritten(String telegram) {
        assertThat(telegrams).contains(telegram);
    }

    private void write(FlightEvent event) {
        WRITERS.stream()
            .filter(writer -> writer.writes(event))
            .map(writer -> writer.write(event, MessageId.create(), REPORTED_AT).getBody())
            .forEach(telegrams::add);
    }
}
