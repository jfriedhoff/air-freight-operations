package be.kdg.airfreight.messaging.infrastructure.messaging;

import be.kdg.airfreight.messaging.domain.DomainRuleViolationException;
import be.kdg.airfreight.messaging.domain.flight.FlightEventKind;

/** The value of "type" in every JSON message; it is also the routing key the message travels with. */
final class MessageTypes {
    static final int VERSION = 1;

    static final String FLIGHT_SCHEDULED = "messaging.flight.scheduled";
    static final String ULD_BOOKING_RECEIVED = "messaging.uld-booking.received";
    static final String CPM_CREATED = "messaging.cpm.created";

    private static final String FLIGHT_OPERATIONS_FLIGHT = "flight-operations.flight.";
    private static final String MESSAGING_FLIGHT = "messaging.flight.";

    private MessageTypes() {
    }

    /** Which flight event a type from Flight Operations stands for. */
    static FlightEventKind kindOf(String type) {
        for (FlightEventKind kind : FlightEventKind.values()) {
            if (type.equals(FLIGHT_OPERATIONS_FLIGHT + nameOf(kind))) {
                return kind;
            }
        }
        throw new DomainRuleViolationException("Unknown flight event type '" + type + "'");
    }

    /** The type under which Messaging passes a flight event on to the other contexts. */
    static String forwardedTypeOf(FlightEventKind kind) {
        return MESSAGING_FLIGHT + nameOf(kind);
    }

    private static String nameOf(FlightEventKind kind) {
        return switch (kind) {
            case ANNOUNCED -> "announced";
            case DEPARTURE_ESTIMATE_CHANGED -> "departure-estimate-changed";
            case ARRIVAL_ESTIMATE_CHANGED -> "arrival-estimate-changed";
            case DEPARTED -> "departed";
            case LANDED -> "landed";
            case REMOVED -> "removed";
        };
    }
}
