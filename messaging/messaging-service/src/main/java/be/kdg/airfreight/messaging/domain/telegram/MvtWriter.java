package be.kdg.airfreight.messaging.domain.telegram;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import be.kdg.airfreight.messaging.domain.AirportCode;
import be.kdg.airfreight.messaging.domain.FlightId;
import be.kdg.airfreight.messaging.domain.FormatVersion;
import be.kdg.airfreight.messaging.domain.MessageId;
import be.kdg.airfreight.messaging.domain.MessageType;
import be.kdg.airfreight.messaging.domain.flight.FlightEvent;
import be.kdg.airfreight.messaging.domain.flight.FlightEventKind;
import be.kdg.airfreight.messaging.domain.outgoingmessage.MessageWriter;
import be.kdg.airfreight.messaging.domain.outgoingmessage.OutgoingMessage;

/**
 * The layout of an MVT telegram, three lines:
 * <pre>
 * MVT
 * HJ221/09.SXBAA.BRU
 * ED0645
 * </pre>
 * A subclass says for which flight event it writes, and supplies the code, the airport and the time.
 */
public abstract class MvtWriter implements MessageWriter<FlightEvent> {
    private static final FormatVersion FORMAT_VERSION = new FormatVersion(1);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HHmm").withZone(ZoneOffset.UTC);

    private final FlightEventKind kind;
    private final MessageType type;
    private final String code;

    protected MvtWriter(FlightEventKind kind, MessageType type, String code) {
        this.kind = kind;
        this.type = type;
        this.code = code;
    }

    @Override
    public final boolean writes(FlightEvent event) {
        return event.kind() == kind;
    }

    @Override
    public final OutgoingMessage write(FlightEvent event, MessageId id, Instant createdAt) {
        FlightId flight = event.flightId();
        String body = "MVT\n"
            + flight.flightNumber().value()
            + "/" + "%02d".formatted(flight.scheduledDepartureDate().getDayOfMonth())
            + "." + event.registration().value()
            + "." + airport(event).value() + "\n"
            + code + TIME.format(time(event));
        return OutgoingMessage.create(id, flight, type, FORMAT_VERSION, createdAt, body);
    }

    protected abstract AirportCode airport(FlightEvent event);

    protected abstract Instant time(FlightEvent event);
}
