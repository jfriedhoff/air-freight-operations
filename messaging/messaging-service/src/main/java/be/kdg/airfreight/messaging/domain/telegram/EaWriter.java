package be.kdg.airfreight.messaging.domain.telegram;

import java.time.Instant;

import be.kdg.airfreight.messaging.domain.AircraftRegistration;
import be.kdg.airfreight.messaging.domain.AirportCode;
import be.kdg.airfreight.messaging.domain.MessageType;
import be.kdg.airfreight.messaging.domain.flight.ArrivalEstimateChanged;

/** MVT-EA: the ETA changed. Names the arrival airport. */
public final class EaWriter extends MvtWriter<ArrivalEstimateChanged> {
    public EaWriter() {
        super(ArrivalEstimateChanged.class, MessageType.MVT_EA, "EA");
    }

    @Override
    protected AircraftRegistration registration(ArrivalEstimateChanged event) {
        return event.registration();
    }

    @Override
    protected AirportCode airport(ArrivalEstimateChanged event) {
        return event.arrivalAirport();
    }

    @Override
    protected Instant time(ArrivalEstimateChanged event) {
        return event.eta();
    }
}
