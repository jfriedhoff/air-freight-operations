package be.kdg.airfreight.messaging.domain.telegram;

import java.time.Instant;

import be.kdg.airfreight.messaging.domain.AircraftRegistration;
import be.kdg.airfreight.messaging.domain.AirportCode;
import be.kdg.airfreight.messaging.domain.MessageType;
import be.kdg.airfreight.messaging.domain.flight.FlightLanded;

/** MVT-AA: the arrival was registered. Names the arrival airport. */
public final class AaWriter extends MvtWriter<FlightLanded> {
    public AaWriter() {
        super(FlightLanded.class, MessageType.MVT_AA, "AA");
    }

    @Override
    protected AircraftRegistration registration(FlightLanded event) {
        return event.registration();
    }

    @Override
    protected AirportCode airport(FlightLanded event) {
        return event.arrivalAirport();
    }

    @Override
    protected Instant time(FlightLanded event) {
        return event.ata();
    }
}
