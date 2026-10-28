package be.kdg.airfreight.messaging.acceptance.unit;

import be.kdg.airfreight.messaging.domain.AirportCode;
import be.kdg.airfreight.messaging.domain.DomainRuleViolationException;
import be.kdg.airfreight.messaging.domain.FlightId;
import be.kdg.airfreight.messaging.domain.FlightNumber;
import be.kdg.airfreight.messaging.domain.FormatVersion;
import be.kdg.airfreight.messaging.domain.MessageId;
import be.kdg.airfreight.messaging.domain.MessageType;
import be.kdg.airfreight.messaging.domain.outgoingmessage.OutgoingMessage;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Story 28. PartnerMessageRelay marks a partner message as sent once it is on the exchange. A message that is
 * marked a second time would reach the partners twice, so OutgoingMessage refuses it. The relay itself is tested
 * by the integration tests.
 */
public class OutgoingMessageSteps {
    private FlightId flight;
    private OutgoingMessage message;
    private Throwable error;

    /** The background of story 28. Only the identity of the flight matters for a partner message. */
    @Given("flight {word} of {date} with STD {time} and STA {time} UTC")
    public void flight(String flightNumber, LocalDate date, LocalTime std, LocalTime sta) {
        flight = new FlightId(new FlightNumber(flightNumber), date, new AirportCode("BRU"));
    }

    @Given("an MVT-ED has been written and sent at {moment} UTC")
    public void anMvtEdHasBeenWrittenAndSent(Instant sentAt) {
        message = OutgoingMessage.create(MessageId.create(), flight, MessageType.MVT_ED, new FormatVersion(1),
            sentAt, "MVT\nHJ221/09.SXBAA.BRU\nED0645");
        message.markSent(sentAt);
    }

    @When("it is marked as sent again at {moment} UTC")
    public void itIsMarkedAsSentAgain(Instant at) {
        error = catchThrowable(() -> message.markSent(at));
    }

    @Then("that is refused, because the message has already been sent")
    public void thatIsRefused() {
        assertThat(error).isInstanceOf(DomainRuleViolationException.class)
            .hasMessageContaining("has already been sent");
    }

    @Then("the message was sent at {moment} UTC")
    public void theMessageWasSentAt(Instant at) {
        assertThat(message.isSent()).isTrue();
        assertThat(message.getSentAt()).isEqualTo(at);
    }
}
