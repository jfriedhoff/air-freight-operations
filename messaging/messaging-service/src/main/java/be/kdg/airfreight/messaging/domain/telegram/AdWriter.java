package be.kdg.airfreight.messaging.domain.telegram;

import java.time.Instant;

import be.kdg.airfreight.messaging.domain.AircraftRegistration;
import be.kdg.airfreight.messaging.domain.AirportCode;
import be.kdg.airfreight.messaging.domain.MessageType;
import be.kdg.airfreight.messaging.domain.flight.FlightDeparted;

/** MVT-AD: the departure was registered. Names the departure airport. */
public final class AdWriter extends MvtWriter<FlightDeparted> {
    public AdWriter() {
        super(FlightDeparted.class, MessageType.MVT_AD, "AD");
    }

    @Override
    protected AircraftRegistration registration(FlightDeparted event) {
        return event.registration();
    }

    @Override
    protected AirportCode airport(FlightDeparted event) {
        return event.flightId().departureAirport();
    }

    @Override
    protected Instant time(FlightDeparted event) {
        return event.atd();
    }
}
