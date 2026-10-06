package be.kdg.airfreight.messaging.domain.telegram;

import java.time.Instant;

import be.kdg.airfreight.messaging.domain.AirportCode;
import be.kdg.airfreight.messaging.domain.DomainRuleViolationException;
import be.kdg.airfreight.messaging.domain.MessageType;
import be.kdg.airfreight.messaging.domain.flight.FlightEvent;
import be.kdg.airfreight.messaging.domain.flight.FlightEventKind;

/** MVT-AD: the departure was registered. Names the departure airport. */
public final class AdWriter extends MvtWriter {
    public AdWriter() {
        super(FlightEventKind.DEPARTED, MessageType.MVT_AD, "AD");
    }

    @Override
    protected AirportCode airport(FlightEvent event) {
        return event.flightId().departureAirport();
    }

    @Override
    protected Instant time(FlightEvent event) {
        Instant atd = event.atd();
        if (atd == null) {
            throw new DomainRuleViolationException("A departed flight needs an ATD");
        }
        return atd;
    }
}
