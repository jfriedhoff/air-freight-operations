package be.kdg.airfreight.messaging.domain.telegram;

import java.time.Instant;

import be.kdg.airfreight.messaging.domain.AirportCode;
import be.kdg.airfreight.messaging.domain.MessageType;
import be.kdg.airfreight.messaging.domain.flight.FlightEvent;
import be.kdg.airfreight.messaging.domain.flight.FlightEventKind;

/** MVT-EA: the ETA changed. Names the arrival airport. */
public final class EaWriter extends MvtWriter {
    public EaWriter() {
        super(FlightEventKind.ARRIVAL_ESTIMATE_CHANGED, MessageType.MVT_EA, "EA");
    }

    @Override
    protected AirportCode airport(FlightEvent event) {
        return event.arrivalAirport();
    }

    @Override
    protected Instant time(FlightEvent event) {
        return event.eta();
    }
}
