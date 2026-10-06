package be.kdg.airfreight.messaging.domain.telegram;

import java.time.Instant;

import be.kdg.airfreight.messaging.domain.AirportCode;
import be.kdg.airfreight.messaging.domain.MessageType;
import be.kdg.airfreight.messaging.domain.flight.FlightEvent;
import be.kdg.airfreight.messaging.domain.flight.FlightEventKind;

/** MVT-ED: the ETD changed. Names the departure airport. */
public final class EdWriter extends MvtWriter {
    public EdWriter() {
        super(FlightEventKind.DEPARTURE_ESTIMATE_CHANGED, MessageType.MVT_ED, "ED");
    }

    @Override
    protected AirportCode airport(FlightEvent event) {
        return event.flightId().departureAirport();
    }

    @Override
    protected Instant time(FlightEvent event) {
        return event.etd();
    }
}
