package be.kdg.airfreight.messaging.domain.telegram;

import java.time.Instant;

import be.kdg.airfreight.messaging.domain.AirportCode;
import be.kdg.airfreight.messaging.domain.DomainRuleViolationException;
import be.kdg.airfreight.messaging.domain.MessageType;
import be.kdg.airfreight.messaging.domain.flight.FlightEvent;
import be.kdg.airfreight.messaging.domain.flight.FlightEventKind;

/** MVT-AA: the arrival was registered. Names the arrival airport. */
public final class AaWriter extends MvtWriter {
    public AaWriter() {
        super(FlightEventKind.LANDED, MessageType.MVT_AA, "AA");
    }

    @Override
    protected AirportCode airport(FlightEvent event) {
        return event.arrivalAirport();
    }

    @Override
    protected Instant time(FlightEvent event) {
        Instant ata = event.ata();
        if (ata == null) {
            throw new DomainRuleViolationException("A landed flight needs an ATA");
        }
        return ata;
    }
}
