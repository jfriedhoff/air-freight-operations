package be.kdg.airfreight.flightoperations.acceptance.unit;

import be.kdg.airfreight.flightoperations.domain.DomainRuleViolationException;
import be.kdg.airfreight.flightoperations.domain.flight.ActualTimeTolerance;
import be.kdg.airfreight.flightoperations.domain.flight.AircraftRegistration;
import be.kdg.airfreight.flightoperations.domain.flight.AirportCode;
import be.kdg.airfreight.flightoperations.domain.flight.DelayCode;
import be.kdg.airfreight.flightoperations.domain.flight.DelayDuration;
import be.kdg.airfreight.flightoperations.domain.flight.DelayKind;
import be.kdg.airfreight.flightoperations.domain.flight.Flight;
import be.kdg.airfreight.flightoperations.domain.flight.FlightNumber;
import be.kdg.airfreight.flightoperations.domain.flight.ScheduledTimes;
import be.kdg.airfreight.flightoperations.domain.flightplanner.FlightPlannerId;
import io.cucumber.java.en.And;
import io.cucumber.java.en.But;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class FlightDelaySteps {
    private static final String HJ221 = "HJ221";
    private static final Instant NOW = Instant.parse("2026-01-09T05:00:00Z");
    private static final String VALID_REASON = "93";
    private static final String UNKNOWN_REASON = "XY";
    private static final String DESCRIPTION = "Late arrival of the aircraft";
    private static final ActualTimeTolerance TOLERANCE = new ActualTimeTolerance(Duration.ofMinutes(5));
    private static final String TOO_LONG = "at most 12 hours";
    private static final String INVALID_REASON = "Delay reason 'XY' is invalid";
    private static final String NO_DESCRIPTION = "description of a delay cannot be blank";
    private static final String NOT_PLANNED = "only change while the flight is planned";
    private static final LocalDate DEPARTURE_DATE = LocalDate.of(2026, Month.JANUARY, 9);
    private static final String LANDED = "no longer change once the flight has landed";

    private final Map<String, Flight> flights = new HashMap<>();
    private final FlightPlannerId planner = FlightPlannerId.create();
    private Throwable error;
    private String expectedRefusal;

    @Given("flight {word} of {int} January {int} is planned with STD {word} and STA {word} UTC")
    public void flightIsPlannedWithStdAndSta(String flightNumber, int day, int year, String std, String sta) {
        plan(flightNumber, utc(day, year, std), utc(day, year, sta));
    }

    @Given("flight {word} of {int} January {int} is planned with STD {word} UTC")
    public void flightIsPlannedWithStd(String flightNumber, int day, int year, String std) {
        Instant departure = utc(day, year, std);
        plan(flightNumber, departure, departure.plus(Duration.ofHours(3)));
    }

    @Given("{word} already has a departure delay of {int} minutes")
    public void flightAlreadyHasADepartureDelay(String flightNumber, int minutes) {
        addDelay(flightNumber, DelayKind.DEPARTURE, minutes);
    }

    @Given("{word} has a departure delay of {int} minutes")
    public void flightHasADepartureDelay(String flightNumber, int minutes) {
        addDelay(flightNumber, DelayKind.DEPARTURE, minutes);
    }

    @Given("flight {word} with ETD {word} and ETA {word} UTC")
    public void flightWithEtdAndEta(String flightNumber, String etd, String eta) {
        plan(flightNumber, onDate(DEPARTURE_DATE, etd), onDate(DEPARTURE_DATE, eta));
    }

    @Given("flight {word} has departed")
    public void flightHasDeparted(String flightNumber) {
        Flight flight = plannedFlight(flightNumber);
        Instant atd = flight.getScheduled().departure();
        flight.recordFinalizedLoad(atd);
        flight.registerDeparture(atd, planner, atd, TOLERANCE);
    }

    @Given("flight {word} has landed")
    public void flightHasLanded(String flightNumber) {
        flightHasDeparted(flightNumber);
        Flight flight = flight(flightNumber);
        Instant ata = flight.getScheduled().arrival();
        flight.registerArrival(ata, planner, ata, TOLERANCE);
    }

    @When("I enter a departure delay of {int} minutes with a valid delay reason and a description")
    public void iEnterAValidDepartureDelay(int minutes) {
        enterDelay(HJ221, DelayKind.DEPARTURE, minutes, VALID_REASON, DESCRIPTION);
    }

    @When("I enter a(n) {word} delay of {int} minutes")
    public void iEnterADelay(String kind, int minutes) {
        expectedRefusal = TOO_LONG;
        enterDelay(HJ221, DelayKind.valueOf(kind.toUpperCase()), minutes, VALID_REASON, DESCRIPTION);
    }

    @When("I enter another departure delay of {int} minutes and an arrival delay of {int} minutes")
    public void iEnterAnotherDepartureDelayAndAnArrivalDelay(int departureMinutes, int arrivalMinutes) {
        addDelay(HJ221, DelayKind.DEPARTURE, departureMinutes);
        addDelay(HJ221, DelayKind.ARRIVAL, arrivalMinutes);
    }

    @When("I enter a departure delay with a delay reason that is not in the IATA list")
    public void iEnterADepartureDelayWithAnUnknownReason() {
        expectedRefusal = INVALID_REASON;
        enterDelay(HJ221, DelayKind.DEPARTURE, 15, UNKNOWN_REASON, DESCRIPTION);
    }

    @When("I enter a departure delay without a description")
    public void iEnterADepartureDelayWithoutADescription() {
        expectedRefusal = NO_DESCRIPTION;
        enterDelay(HJ221, DelayKind.DEPARTURE, 15, VALID_REASON, "");
    }

    @When("I enter a departure delay of {int} minutes for {word}")
    public void iEnterADepartureDelayFor(int minutes, String flightNumber) {
        enterDelay(flightNumber, DelayKind.DEPARTURE, minutes, VALID_REASON, DESCRIPTION);
    }

    @When("I enter a departure delay")
    public void iEnterADepartureDelay() {
        expectedRefusal = NOT_PLANNED;
        enterDelay(HJ221, DelayKind.DEPARTURE, 15, VALID_REASON, DESCRIPTION);
    }

    @When("I enter an arrival delay")
    public void iEnterAnArrivalDelay() {
        expectedRefusal = LANDED;
        enterDelay(HJ221, DelayKind.ARRIVAL, 15, VALID_REASON, DESCRIPTION);
    }

    @Then("the ETD is {word}")
    public void theEtdIs(String time) {
        Flight flight = flight(HJ221);
        assertThat(flight.getEstimated().departure()).isEqualTo(onDepartureDate(flight, time));
    }

    @Then("the ETD remains {word}")
    public void theEtdRemains(String time) {
        assertThat(error).isNull();
        theEtdIs(time);
    }

    @And("the ETA is {word}")
    public void theEtaIs(String time) {
        Flight flight = flight(HJ221);
        assertThat(flight.getEstimated().arrival()).isEqualTo(onDepartureDate(flight, time));
    }

    @And("it is recorded that I entered the delay")
    public void itIsRecordedThatIEnteredTheDelay() {
        assertThat(flight(HJ221).getDelays().getLast().getLastChangedBy()).isEqualTo(planner);
    }

    @Then("it is {word}")
    public void itIs(String result) {
        switch (result) {
            case "accepted" -> assertThat(error).isNull();
            case "refused" -> assertRefusedForTheExpectedReason();
            default -> throw new IllegalArgumentException("Unknown result: " + result);
        }
    }

    @Then("it is refused with an explanation")
    public void itIsRefusedWithAnExplanation() {
        assertRefusedForTheExpectedReason();
    }

    @Then("the ETD of {word} is {int} January {int} {word} UTC")
    public void theEtdOfFlightIs(String flightNumber, int day, int year, String time) {
        assertThat(error).isNull();
        assertThat(flight(flightNumber).getEstimated().departure()).isEqualTo(utc(day, year, time));
    }

    @But("it remains the flight {word} of {int} January {int}")
    public void itRemainsTheFlight(String flightNumber, int day, int year) {
        Flight flight = flight(flightNumber);
        assertThat(flight.getFlightNumber()).isEqualTo(new FlightNumber(flightNumber));
        assertThat(flight.getScheduled().departureDate()).isEqualTo(LocalDate.of(year, Month.JANUARY, day));
    }

    private void plan(String flightNumber, Instant std, Instant sta) {
        flights.put(flightNumber, newFlight(flightNumber, std, sta));
    }

    /** Story 9 starts without a Background: plan the flight like story 8 does if it doesn't exist yet. */
    private Flight plannedFlight(String flightNumber) {
        return flights.computeIfAbsent(flightNumber, number ->
            newFlight(number, onDate(DEPARTURE_DATE, "06:30"), onDate(DEPARTURE_DATE, "09:45")));
    }

    private static Flight newFlight(String flightNumber, Instant std, Instant sta) {
        return Flight.plan(new FlightNumber(flightNumber), new AirportCode("BRU"), new AirportCode("ATH"),
            new AircraftRegistration("SXBAA"), new ScheduledTimes(std, sta));
    }

    private void addDelay(String flightNumber, DelayKind kind, int minutes) {
        flight(flightNumber).addDelay(kind, new DelayDuration(minutes), new DelayCode(VALID_REASON), DESCRIPTION,
            planner, NOW);
    }

    private void enterDelay(String flightNumber, DelayKind kind, int minutes, String reason, String description) {
        error = catchThrowable(() -> flight(flightNumber).addDelay(kind, new DelayDuration(minutes),
            new DelayCode(reason), description, planner, NOW));
    }

    private void assertRefusedForTheExpectedReason() {
        assertThat(error)
            .isInstanceOf(DomainRuleViolationException.class)
            .hasMessageContaining(expectedRefusal);
    }

    private Flight flight(String flightNumber) {
        return flights.get(flightNumber);
    }

    private static Instant utc(int day, int year, String time) {
        return onDate(LocalDate.of(year, Month.JANUARY, day), time);
    }

    private static Instant onDepartureDate(Flight flight, String time) {
        return onDate(flight.getScheduled().departureDate(), time);
    }

    private static Instant onDate(LocalDate date, String time) {
        return date.atTime(LocalTime.parse(time)).toInstant(ZoneOffset.UTC);
    }
}
