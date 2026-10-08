package be.kdg.airfreight.messaging.acceptance.unit;

import be.kdg.airfreight.messaging.domain.DomainRuleViolationException;
import be.kdg.airfreight.messaging.domain.telegram.DateDerivation;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Story 30. "A ULD message is processed" calls the domain: DateDerivation derives the date, as BookingService
 * does after JsonUldMessageReader has read the message. A refused derivation is what makes BookingService set
 * the message aside. Reading, the inbox and setting aside are tested by the integration tests.
 */
public class DateDerivationSteps {
    private final DateDerivation derivation = new DateDerivation();

    private Instant now = Instant.parse("2026-01-09T08:00:00Z");
    private int dayOfMonth;
    private LocalTime time;
    private LocalDate date;
    private Instant moment;
    private Throwable error;

    @Given("it is {moment} UTC")
    public void itIs(Instant moment) {
        now = moment;
    }

    @Given("it is {date}")
    public void itIs(LocalDate date) {
        now = date.atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    @Given("the day of the month {day} and the time {hhmm}")
    public void theDayOfTheMonthAndTheTime(int dayOfMonth, LocalTime time) {
        this.dayOfMonth = dayOfMonth;
        this.time = time;
    }

    @When("they are read on {moment}")
    public void theyAreReadOn(Instant readAt) {
        date = derivation.deriveDay(dayOfMonth, readAt);
        moment = derivation.deriveTime(time, readAt);
    }

    @When("the day of the month {day} is derived")
    @When("a ULD message with day {day} is processed")
    public void theDayOfTheMonthIsDerived(int dayOfMonth) {
        error = catchThrowable(() -> date = derivation.deriveDay(dayOfMonth, now));
    }

    @When("the time {hhmm} is derived")
    public void theTimeIsDerived(LocalTime time) {
        moment = derivation.deriveTime(time, now);
    }

    @Then("they concern the flight of {date} and the moment {moment}")
    public void theyConcern(LocalDate flight, Instant expected) {
        assertThat(date).isEqualTo(flight);
        assertThat(moment).isEqualTo(expected);
    }

    @Then("the date is {date}")
    @Then("it concerns the flight of {date}")
    public void theDateIs(LocalDate expected) {
        assertThat(error).isNull();
        assertThat(date).isEqualTo(expected);
    }

    /** Also checks the reasoning of the scenario, so a wrong count in the Gherkin fails too. */
    @Then("the date is {date}, because it lies {int} days back and {int} {month} lies {int} days ahead")
    public void theDateIsBecause(LocalDate expected, int daysBack, int otherDay, Month otherMonth, int daysAhead) {
        theDateIs(expected);
        LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);
        assertThat(ChronoUnit.DAYS.between(expected, today)).isEqualTo(daysBack);
        assertThat(ChronoUnit.DAYS.between(today, LocalDate.of(today.getYear(), otherMonth, otherDay)))
            .isEqualTo(daysAhead);
    }

    @Then("the moment is {moment} UTC")
    public void theMomentIs(Instant expected) {
        assertThat(moment).isEqualTo(expected);
    }

    @Then("the message is invalid")
    public void theMessageIsInvalid() {
        assertThat(error).isInstanceOf(DomainRuleViolationException.class).hasMessageContaining("is invalid");
    }
}
