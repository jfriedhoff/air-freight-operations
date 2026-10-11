package be.kdg.airfreight.messaging.acceptance.integration;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Valid messages as their senders write them, for HJ221 from BRU to ATH on 9 January 2026, STD 06:30 and
 * STA 09:45 UTC. Every message is a new map with a new messageId, so a scenario can change or drop a field.
 */
final class Messages {
    static final String FLIGHT_NUMBER = "HJ221";
    static final LocalDate DATE = LocalDate.of(2026, 1, 9);
    static final Instant STD = Instant.parse("2026-01-09T06:30:00Z");
    static final Instant STA = Instant.parse("2026-01-09T09:45:00Z");
    static final UUID FLIGHT_REFERENCE = UUID.fromString("7d5e4f1a-0c3b-4e2a-9f6d-1b2c3d4e5f60");

    private Messages() {
    }

    /** Contract cargo-booking.uld: AKE16689R7 of 1250 kg for ATH on HJ221, day 09. */
    static Map<String, Object> uldMessage() {
        Map<String, Object> message = envelope("cargo-booking.uld");
        message.put("createdAt", "2026-01-02T12:00:00Z");
        message.put("flightNumber", FLIGHT_NUMBER);
        message.put("dayOfMonth", 9);
        message.put("departureAirport", "BRU");
        message.put("uldCode", "AKE16689R7");
        message.put("weight", 1250);
        message.put("weightUnit", "KG");
        message.put("destination", "ATH");
        return message;
    }

    /** Contract network-planning.ssm: HJ221 on Mondays from 5 January 2026 until the given date. */
    static Map<String, Object> ssm(LocalDate from, LocalDate to) {
        Map<String, Object> message = envelope("network-planning.ssm");
        message.put("createdAt", "2026-01-02T12:00:00Z");
        message.put("periodFrom", from.toString());
        message.put("periodTo", to.toString());
        message.put("weekdays", List.of(from.getDayOfWeek().name()));
        message.put("departureTime", "06:30");
        message.put("arrivalTime", "09:45");
        message.put("flightNumber", FLIGHT_NUMBER);
        message.put("departureAirport", "BRU");
        message.put("arrivalAirport", "ATH");
        message.put("registration", "SXBAA");
        return message;
    }

    /** Contract flight-operations.flight.{name}, with the fields every flight event has. */
    static Map<String, Object> flightEvent(String name) {
        Map<String, Object> message = envelope("flight-operations.flight." + name);
        message.put("occurredOn", "2026-01-08T12:00:00Z");
        message.put("flightId", FLIGHT_REFERENCE.toString());
        message.put("flightNumber", FLIGHT_NUMBER);
        message.put("scheduledDepartureDate", DATE.toString());
        message.put("departureAirport", "BRU");
        message.put("arrivalAirport", "ATH");
        message.put("registration", "SXBAA");
        return message;
    }

    /** Flight Operations announces a new flight. */
    static Map<String, Object> flightAnnounced(String flightNumber, LocalDate date, String departureAirport,
                                               String arrivalAirport, String std, String sta) {
        Map<String, Object> message = flightEvent("announced");
        message.put("flightId", UUID.randomUUID().toString());
        message.put("flightNumber", flightNumber);
        message.put("scheduledDepartureDate", date.toString());
        message.put("departureAirport", departureAirport);
        message.put("arrivalAirport", arrivalAirport);
        message.put("std", std);
        message.put("sta", sta);
        return message;
    }

    /** Contract load-planning.flight-cargo.load-finalized: AKE16689R7 on CL. */
    static Map<String, Object> loadFinalized() {
        Map<String, Object> message = envelope("load-planning.flight-cargo.load-finalized");
        message.put("occurredOn", "2026-01-09T06:00:00Z");
        message.put("flightNumber", FLIGHT_NUMBER);
        message.put("scheduledDepartureDate", DATE.toString());
        message.put("departureAirport", "BRU");
        message.put("load", List.of(Map.of("uldCode", "AKE16689R7", "position", "CL", "weight", 1250,
            "weightUnit", "KG", "destination", "ATH")));
        return message;
    }

    static UUID idOf(Map<String, Object> message) {
        return UUID.fromString(String.valueOf(message.get("messageId")));
    }

    private static Map<String, Object> envelope(String type) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("messageId", UUID.randomUUID().toString());
        message.put("type", type);
        message.put("version", 1);
        return message;
    }
}
