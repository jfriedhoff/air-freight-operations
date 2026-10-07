package be.kdg.airfreight.messaging.domain.telegram;

import java.time.Instant;

import be.kdg.airfreight.messaging.domain.AircraftRegistration;
import be.kdg.airfreight.messaging.domain.AirportCode;
import be.kdg.airfreight.messaging.domain.MessageType;
import be.kdg.airfreight.messaging.domain.flight.DepartureEstimateChanged;

/** MVT-ED: the ETD changed. Names the departure airport. */
public final class EdWriter extends MvtWriter<DepartureEstimateChanged> {
    public EdWriter() {
        super(DepartureEstimateChanged.class, MessageType.MVT_ED, "ED");
    }

    @Override
    protected AircraftRegistration registration(DepartureEstimateChanged event) {
        return event.registration();
    }

    @Override
    protected AirportCode airport(DepartureEstimateChanged event) {
        return event.flightId().departureAirport();
    }

    @Override
    protected Instant time(DepartureEstimateChanged event) {
        return event.etd();
    }
}
