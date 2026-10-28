package be.kdg.airfreight.messaging.acceptance.integration;

import io.cucumber.java.en.Given;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/** The flight of a scenario. Messaging keeps no flights: the flight is the one in the messages the test sends. */
public class FlightSteps {
    @Given("flight {word} of {date} with STD {time} and STA {time} UTC")
    public void flight(String flightNumber, LocalDate date, LocalTime std, LocalTime sta) {
        assertThat(flightNumber).isEqualTo(Messages.FLIGHT_NUMBER);
        assertThat(date).isEqualTo(Messages.DATE);
        assertThat(Messages.STD).isEqualTo(utc(date, std));
        assertThat(Messages.STA).isEqualTo(utc(date, sta));
    }

    /** The background of stories 11 and 14. Messages writes every flight event for this flight. */
    @Given("flight {word} of {date} with registration {word} from {word} to {word}")
    public void flight(String flightNumber, LocalDate date, String registration, String from, String to) {
        assertThat(flightNumber).isEqualTo(Messages.FLIGHT_NUMBER);
        assertThat(date).isEqualTo(Messages.DATE);
        assertThat(registration).isEqualTo("SXBAA");
        assertThat(from).isEqualTo("BRU");
        assertThat(to).isEqualTo("ATH");
    }

    @Given("the STD is {time} and the STA {time} UTC")
    public void theScheduledTimes(LocalTime std, LocalTime sta) {
        assertThat(Messages.STD).isEqualTo(utc(Messages.DATE, std));
        assertThat(Messages.STA).isEqualTo(utc(Messages.DATE, sta));
    }

    private static Instant utc(LocalDate date, LocalTime time) {
        return date.atTime(time).toInstant(ZoneOffset.UTC);
    }
}
