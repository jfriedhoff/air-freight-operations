package be.kdg.airfreight.loadplanning.infrastructure.messaging;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadFinalized;

/**
 * Contract load-planning.flight-cargo.load-finalized, version 1: the published language of Load
 * Planning. Other contexts read this JSON: change it only with a new version.
 */
record LoadFinalizedJson(UUID messageId, String type, int version, Instant occurredOn, String flightNumber,
                         LocalDate scheduledDepartureDate, String departureAirport, List<UldLineJson> load) {
    static final int VERSION = 1;

    static LoadFinalizedJson from(LoadFinalized event, UUID messageId) {
        return new LoadFinalizedJson(messageId, MessagingConfig.LOAD_FINALIZED, VERSION, event.occurredOn(),
            event.flightId().flightNumber().value(), event.flightId().scheduledDepartureDate(),
            event.flightId().departureAirport().value(),
            event.load().stream().map(UldLineJson::from).toList());
    }
}
