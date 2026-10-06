package be.kdg.airfreight.messaging.domain.telegram;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import be.kdg.airfreight.messaging.domain.AircraftRegistration;
import be.kdg.airfreight.messaging.domain.AirportCode;
import be.kdg.airfreight.messaging.domain.FlightId;
import be.kdg.airfreight.messaging.domain.FormatVersion;
import be.kdg.airfreight.messaging.domain.MessageId;
import be.kdg.airfreight.messaging.domain.MessageType;
import be.kdg.airfreight.messaging.domain.flight.FlightEvent;
import be.kdg.airfreight.messaging.domain.outgoingmessage.MessageWriter;
import be.kdg.airfreight.messaging.domain.outgoingmessage.OutgoingMessage;

/**
 * The layout of an MVT telegram, three lines:
 * <pre>
 * MVT
 * HJ221/09.SXBAA.BRU
 * ED0645
 * </pre>
 * A subclass names the flight event it writes for, and supplies the code, the registration, the airport
 * and the time from that event.
 */
public abstract class MvtWriter<T extends FlightEvent> implements MessageWriter<FlightEvent> {
    private static final FormatVersion FORMAT_VERSION = new FormatVersion(1);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HHmm").withZone(ZoneOffset.UTC);

    private final Class<T> eventType;
    private final MessageType type;
    private final String code;

    protected MvtWriter(Class<T> eventType, MessageType type, String code) {
        this.eventType = eventType;
        this.type = type;
        this.code = code;
    }

    @Override
    public final boolean writes(FlightEvent event) {
        return eventType.isInstance(event);
    }

    @Override
    public final OutgoingMessage write(FlightEvent flightEvent, MessageId id, Instant createdAt) {
        T event = eventType.cast(flightEvent);
        FlightId flight = event.flightId();
        String body = "MVT\n"
            + flight.flightNumber().value()
            + "/" + "%02d".formatted(flight.scheduledDepartureDate().getDayOfMonth())
            + "." + registration(event).value()
            + "." + airport(event).value() + "\n"
            + code + TIME.format(time(event));
        return OutgoingMessage.create(id, flight, type, FORMAT_VERSION, createdAt, body);
    }

    protected abstract AircraftRegistration registration(T event);

    protected abstract AirportCode airport(T event);

    protected abstract Instant time(T event);
}
