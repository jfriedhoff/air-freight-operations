package be.kdg.airfreight.messaging.acceptance.unit;

import be.kdg.airfreight.messaging.domain.AircraftRegistration;
import be.kdg.airfreight.messaging.domain.AirportCode;
import be.kdg.airfreight.messaging.domain.FlightNumber;
import be.kdg.airfreight.messaging.domain.schedule.FlightScheduled;
import be.kdg.airfreight.messaging.domain.schedule.FlightSeries;
import be.kdg.airfreight.messaging.domain.schedule.SchedulePeriod;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Story 1. "The SSM is processed" calls the domain: FlightSeries turns the series into flights, as
 * ScheduleService does after JsonSsmReader has read the message. Reading the JSON is tested by the integration
 * tests. What a scenario does not name gets a default value.
 */
public class ScheduleSteps {
    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");
    private static final String DEPARTURE_AIRPORT = "BRU";
    private static final String ARRIVAL_AIRPORT = "ATH";
    private static final LocalTime DEPARTURE = LocalTime.of(6, 30);
    private static final LocalTime ARRIVAL = LocalTime.of(9, 45);
    private static final String REGISTRATION = "SXBAA";

    private FlightSeries series;
    private List<FlightScheduled> flights;

    @Given("an SSM for {word} from {word} to {word} from {int} up to and including {int} {month} {int} "
        + "on {weekday} and {weekday}, departure {time} and arrival {time} UTC, registration {word}")
    public void anSsmForAPeriod(String flightNumber, String from, String to, int firstDay, int lastDay, Month month,
                                int year, DayOfWeek weekday, DayOfWeek otherWeekday, LocalTime departure,
                                LocalTime arrival, String registration) {
        series = new FlightSeries(
            new SchedulePeriod(LocalDate.of(year, month, firstDay), LocalDate.of(year, month, lastDay)),
            Set.of(weekday, otherWeekday), departure, arrival, new FlightNumber(flightNumber),
            new AirportCode(from), new AirportCode(to), new AircraftRegistration(registration));
    }

    @Given("an SSM for {word} on {weekday} {int} {month} {int} with departure {time} and arrival {time} UTC")
    public void anSsmForOneDay(String flightNumber, DayOfWeek weekday, int day, Month month, int year,
                               LocalTime departure, LocalTime arrival) {
        LocalDate date = dateOn(weekday, day, month, year);
        series = seriesWithDefaults(flightNumber, date, date, weekday, departure, arrival);
    }

    @Given("an SSM for {word} on {weekday} {int} {month} {int} with departure and arrival at {time} UTC")
    public void anSsmForOneDayArrivingAtTheSameTime(String flightNumber, DayOfWeek weekday, int day, Month month,
                                                    int year, LocalTime time) {
        anSsmForOneDay(flightNumber, weekday, day, month, year, time, time);
    }

    @Given("an SSM for {word} from {weekday} {int} up to and including {weekday} {int} {month} {int} on {weekday}")
    public void anSsmForAPeriodOnOneWeekday(String flightNumber, DayOfWeek firstWeekday, int firstDay,
                                            DayOfWeek lastWeekday, int lastDay, Month month, int year,
                                            DayOfWeek weekday) {
        series = seriesWithDefaults(flightNumber, dateOn(firstWeekday, firstDay, month, year),
            dateOn(lastWeekday, lastDay, month, year), weekday, DEPARTURE, ARRIVAL);
    }

    @When("the SSM is processed")
    public void theSsmIsProcessed() {
        flights = series.flightsScheduled(NOW);
    }

    @Then("flights {word} from {word} exist on {days} {month} {int}")
    public void flightsExistOn(String flightNumber, String from, List<Integer> days, Month month, int year) {
        assertThat(flights).allSatisfy(flight -> {
            assertThat(flight.flightNumber()).isEqualTo(new FlightNumber(flightNumber));
            assertThat(flight.departureAirport()).isEqualTo(new AirportCode(from));
        });
        assertThat(flights).extracting(ScheduleSteps::departureDate)
            .containsExactlyElementsOf(days.stream().map(day -> LocalDate.of(year, month, day)).toList());
    }

    @Then("each flight has an STD of {time} and an STA of {time} on its date")
    public void eachFlightHasAnStdAndStaOnItsDate(LocalTime std, LocalTime sta) {
        assertThat(flights).isNotEmpty().allSatisfy(flight -> {
            LocalDate date = departureDate(flight);
            assertThat(flight.std()).isEqualTo(utc(date, std));
            assertThat(flight.sta()).isEqualTo(utc(date, sta));
        });
    }

    @Then("each flight has registration {word}")
    public void eachFlightHasRegistration(String registration) {
        assertThat(flights).isNotEmpty()
            .allSatisfy(flight -> assertThat(flight.registration()).isEqualTo(new AircraftRegistration(registration)));
    }

    @Then("the flight of {int} {month} has an STA on {int} {month} {int} at {time}")
    public void theFlightHasAnStaOn(int day, Month month, int staDay, Month staMonth, int year, LocalTime sta) {
        assertThat(flights).singleElement().satisfies(flight -> {
            assertThat(departureDate(flight)).isEqualTo(LocalDate.of(year, month, day));
            assertThat(flight.sta()).isEqualTo(utc(LocalDate.of(year, staMonth, staDay), sta));
        });
    }

    @Then("no flights are created")
    public void noFlightsAreCreated() {
        assertThat(flights).isEmpty();
    }

    private static FlightSeries seriesWithDefaults(String flightNumber, LocalDate from, LocalDate to,
                                                   DayOfWeek weekday, LocalTime departure, LocalTime arrival) {
        return new FlightSeries(new SchedulePeriod(from, to), Set.of(weekday), departure, arrival,
            new FlightNumber(flightNumber), new AirportCode(DEPARTURE_AIRPORT), new AirportCode(ARRIVAL_AIRPORT),
            new AircraftRegistration(REGISTRATION));
    }

    /** "Monday 5 January 2026": also checks that the scenario names the right weekday. */
    private static LocalDate dateOn(DayOfWeek weekday, int day, Month month, int year) {
        LocalDate date = LocalDate.of(year, month, day);
        assertThat(date.getDayOfWeek()).as("weekday of %s", date).isEqualTo(weekday);
        return date;
    }

    private static LocalDate departureDate(FlightScheduled flight) {
        return LocalDate.ofInstant(flight.std(), ZoneOffset.UTC);
    }

    private static Instant utc(LocalDate date, LocalTime time) {
        return date.atTime(time).toInstant(ZoneOffset.UTC);
    }
}
