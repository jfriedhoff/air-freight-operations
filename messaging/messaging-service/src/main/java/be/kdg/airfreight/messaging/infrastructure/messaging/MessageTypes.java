package be.kdg.airfreight.messaging.infrastructure.messaging;

import be.kdg.airfreight.messaging.domain.DomainRuleViolationException;
import be.kdg.airfreight.messaging.domain.flight.ArrivalEstimateChanged;
import be.kdg.airfreight.messaging.domain.flight.DepartureEstimateChanged;
import be.kdg.airfreight.messaging.domain.flight.FlightAnnounced;
import be.kdg.airfreight.messaging.domain.flight.FlightDeparted;
import be.kdg.airfreight.messaging.domain.flight.FlightEvent;
import be.kdg.airfreight.messaging.domain.flight.FlightLanded;
import be.kdg.airfreight.messaging.domain.flight.FlightRemoved;

/** The value of "type" in every JSON message; it is also the routing key the message travels with. */
final class MessageTypes {
    static final int VERSION = 1;

    static final String FLIGHT_SCHEDULED = "messaging.flight.scheduled";
    static final String ULD_BOOKING_RECEIVED = "messaging.uld-booking.received";
    static final String CPM_CREATED = "messaging.cpm.created";

    /** The last part of a flight event type: flight-operations.flight.announced, messaging.flight.announced. */
    static final String ANNOUNCED = "announced";
    static final String DEPARTURE_ESTIMATE_CHANGED = "departure-estimate-changed";
    static final String ARRIVAL_ESTIMATE_CHANGED = "arrival-estimate-changed";
    static final String DEPARTED = "departed";
    static final String LANDED = "landed";
    static final String REMOVED = "removed";

    private static final String FLIGHT_OPERATIONS_FLIGHT = "flight-operations.flight.";
    private static final String MESSAGING_FLIGHT = "messaging.flight.";

    private MessageTypes() {
    }

    /** Which flight event a type from Flight Operations names: its last part. */
    static String flightEventNameOf(String type) {
        if (!type.startsWith(FLIGHT_OPERATIONS_FLIGHT)) {
            throw new DomainRuleViolationException("Unknown flight event type '" + type + "'");
        }
        return type.substring(FLIGHT_OPERATIONS_FLIGHT.length());
    }

    /** The type under which Messaging passes a flight event on to the other contexts. */
    static String forwardedTypeOf(FlightEvent event) {
        return MESSAGING_FLIGHT + switch (event) {
            case FlightAnnounced announced -> ANNOUNCED;
            case DepartureEstimateChanged changed -> DEPARTURE_ESTIMATE_CHANGED;
            case ArrivalEstimateChanged changed -> ARRIVAL_ESTIMATE_CHANGED;
            case FlightDeparted departed -> DEPARTED;
            case FlightLanded landed -> LANDED;
            case FlightRemoved removed -> REMOVED;
        };
    }
}
