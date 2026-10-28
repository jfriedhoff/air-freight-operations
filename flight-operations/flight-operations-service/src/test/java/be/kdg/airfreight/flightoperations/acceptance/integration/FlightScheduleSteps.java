package be.kdg.airfreight.flightoperations.acceptance.integration;

import be.kdg.airfreight.flightoperations.TestHelper;
import be.kdg.airfreight.flightoperations.application.FlightSchedulingService;
import be.kdg.airfreight.flightoperations.application.command.ScheduleFlightCommand;
import be.kdg.airfreight.flightoperations.domain.flight.AircraftRegistration;
import be.kdg.airfreight.flightoperations.domain.flight.AirportCode;
import be.kdg.airfreight.flightoperations.domain.flight.Flight;
import be.kdg.airfreight.flightoperations.domain.flight.FlightNumber;
import ch.qos.logback.classic.Level;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Story #2: the flights that Messaging takes from an SSM, as Flight Operations receives them. Messaging
 * splits an SSM into one message per flight (story #1). These steps call the use case that
 * MessagingEventListener calls for each of those messages: the tests read nothing from RabbitMQ, and the
 * architecture test allows no code but the listener to call it from infrastructure.
 */
public class FlightScheduleSteps {
    /** The stories name the day and month; the year is the year of the flight that already exists. */
    private static final int YEAR = 2026;
    private static final LocalTime STD = LocalTime.of(6, 30);
    private static final String REGISTRATION = "SXBAA";
    private static final String ANNOUNCED = "flight-operations.flight.announced";
    private static final String ALREADY_EXISTS = "already exists";

    private final TestHelper testHelper;
    private final FlightSchedulingService scheduling;
    private final LogCapture logs;

    /** The flight that exists before the SSM arrives. */
    private String flightNumber;
    private String departureAirport;
    private LocalDate date;
    private final List<Runnable> announcements = new ArrayList<>();

    public FlightScheduleSteps(TestHelper testHelper, FlightSchedulingService scheduling, LogCapture logs) {
        this.testHelper = testHelper;
        this.scheduling = scheduling;
        this.logs = logs;
    }

    @Given("the flight {word} of {date} from {word} already exists")
    public void theFlightAlreadyExists(String flightNumber, LocalDate date, String departureAirport) {
        theFlightExistsWith(flightNumber, date, departureAirport, STD, REGISTRATION);
    }

    @Given("the flight {word} of {date} from {word} exists with STD {time} UTC and registration {word}")
    public void theFlightExistsWith(String flightNumber, LocalDate date, String departureAirport, LocalTime std,
                                    String registration) {
        this.flightNumber = flightNumber;
        this.departureAirport = departureAirport;
        this.date = date;
        testHelper.saveFlight(flightNumber, departureAirport, date, std, registration);
    }

    @When("an SSM is processed that announces {word} from {word} on {days} {month}")
    public void anSsmIsProcessed(String flightNumber, String departureAirport, List<Integer> days, Month month) {
        anSsmIsProcessedWith(flightNumber, departureAirport, days, month, STD, REGISTRATION);
    }

    @When("an SSM is processed that announces {word} from {word} on {days} {month} with STD {time} UTC and registration {word}")
    public void anSsmIsProcessedWith(String flightNumber, String departureAirport, List<Integer> days, Month month,
                                     LocalTime std, String registration) {
        for (int day : days) {
            announce(flightNumber, departureAirport, LocalDate.of(YEAR, month, day), std,
                registration);
        }
    }

    /** Two SSMs that differ (another registration) but announce the same flight. */
    @Given("two different SSMs both announce {word} from {word} on {date}")
    public void twoDifferentSsmsBothAnnounce(String flightNumber, String departureAirport, LocalDate date) {
        announcements.add(() -> announce(flightNumber, departureAirport, date, STD, "SXBAA"));
        announcements.add(() -> announce(flightNumber, departureAirport, date, STD, "SXBAB"));
    }

    /**
     * Both start together on their own thread. The database allows the flight once, so one of them can fail;
     * that one is delivered again, as the retries of the listener container do.
     */
    @When("Flight Operations processes both announcements at the same time")
    public void flightOperationsProcessesBothAtTheSameTime() {
        CountDownLatch start = new CountDownLatch(1);
        List<CompletableFuture<Void>> running = announcements.stream()
            .map(announcement -> CompletableFuture.runAsync(() -> {
                await(start);
                try {
                    announcement.run();
                } catch (RuntimeException failed) {
                    announcement.run();
                }
            }))
            .toList();
        start.countDown();
        running.forEach(CompletableFuture::join);
    }

    @Then("only the flight of {int} {month} is created")
    public void onlyTheFlightOfIsCreated(int day, Month month) {
        assertThat(testHelper.findFlight(flightNumber, LocalDate.of(YEAR, month, day), departureAirport)).isPresent();
        assertThat(testHelper.flightCount()).as("flights").isEqualTo(2);
        assertThat(testHelper.outboxTypes()).containsExactly(ANNOUNCED);
    }

    @Then("the duplicate of {int} {month} is logged with flight number, date and departure airport")
    public void theDuplicateIsLoggedWithFlightNumberDateAndDepartureAirport(int day, Month month) {
        logs.assertLogged(Level.INFO, flightNumber, LocalDate.of(YEAR, month, day).toString(), departureAirport,
            ALREADY_EXISTS);
    }

    @Then("the duplicate is logged")
    public void theDuplicateIsLogged() {
        logs.assertLogged(Level.INFO, ALREADY_EXISTS);
    }

    @Then("the flight still has STD {time} UTC and registration {word}")
    public void theFlightStillHas(LocalTime std, String registration) {
        Flight flight = testHelper.findFlight(flightNumber, date, departureAirport).orElseThrow();
        assertThat(flight.getScheduled().departure()).isEqualTo(date.atTime(std).toInstant(ZoneOffset.UTC));
        assertThat(flight.getRegistration().value()).isEqualTo(registration);
        assertThat(testHelper.outboxTypes()).isEmpty();
    }

    @Then("the flight {word} of {int} {month} from {word} is created")
    public void theFlightIsCreated(String flightNumber, int day, Month month, String departureAirport) {
        assertThat(testHelper.findFlight(flightNumber, LocalDate.of(YEAR, month, day), departureAirport)).isPresent();
        assertThat(testHelper.outboxTypes()).containsExactly(ANNOUNCED);
    }

    @Then("the flight {word} of {int} {month} from {word} still exists")
    public void theFlightStillExists(String flightNumber, int day, Month month, String departureAirport) {
        assertThat(testHelper.findFlight(flightNumber, LocalDate.of(YEAR, month, day), departureAirport)).isPresent();
    }

    @Then("the flight {word} of {int} {month} from {word} exists exactly once")
    public void theFlightExistsExactlyOnce(String flightNumber, int day, Month month, String departureAirport) {
        assertThat(testHelper.findFlight(flightNumber, LocalDate.of(YEAR, month, day), departureAirport)).isPresent();
        assertThat(testHelper.flightCount()).as("flights").isEqualTo(1);
    }

    @Then("it is announced exactly once")
    public void itIsAnnouncedExactlyOnce() {
        assertThat(testHelper.outboxTypes()).containsExactly(ANNOUNCED);
    }

    /** What MessagingEventListener makes of a messaging.flight.scheduled message. */
    private void announce(String flightNumber, String departureAirport, LocalDate date, LocalTime std,
                          String registration) {
        scheduling.scheduleFlight(new ScheduleFlightCommand(new FlightNumber(flightNumber),
            new AirportCode(departureAirport), new AirportCode(TestHelper.otherAirport(departureAirport)),
            new AircraftRegistration(registration), TestHelper.schedule(date, std)));
    }

    private static void await(CountDownLatch start) {
        try {
            start.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
