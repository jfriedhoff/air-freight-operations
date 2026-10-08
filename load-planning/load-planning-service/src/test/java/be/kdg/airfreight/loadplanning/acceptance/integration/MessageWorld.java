package be.kdg.airfreight.loadplanning.acceptance.integration;

import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargo;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargoRepository;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightId;
import io.cucumber.spring.ScenarioScope;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The state of one scenario, shared by its step classes. Spring makes a new one for every scenario and
 * hands the step classes a proxy to it: use the methods, a field of the proxy is not the real field.
 */
@Component
@ScenarioScope
public class MessageWorld {
    private final FlightCargoRepository cargos;

    /** The flight of the scenario: "that flight", "the same flight". */
    private FlightId flight;
    /** The last ULD message, and a fragment of the reason it must be set aside for. */
    private UUID messageId;
    private String expectedReason;

    public MessageWorld(FlightCargoRepository cargos) {
        this.cargos = cargos;
    }

    public FlightId getFlight() {
        return flight;
    }

    public void setFlight(FlightId flight) {
        this.flight = flight;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public void setMessageId(UUID messageId) {
        this.messageId = messageId;
    }

    public String getExpectedReason() {
        return expectedReason;
    }

    public void setExpectedReason(String expectedReason) {
        this.expectedReason = expectedReason;
    }

    public void assertFlight(String flightNumber, LocalDate date) {
        assertThat(flight.flightNumber().value()).as("flight").isEqualTo(flightNumber);
        assertThat(flight.scheduledDepartureDate()).as("scheduled departure date").isEqualTo(date);
    }

    /** The flight of the scenario as Load Planning has stored it. */
    public FlightCargo cargo() {
        return cargos.findByFlightId(flight).orElseThrow(() -> new AssertionError("Load Planning has no " + flight));
    }

    public boolean exists(FlightId other) {
        return cargos.findByFlightId(other).isPresent();
    }
}
