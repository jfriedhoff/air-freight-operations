package be.kdg.airfreight.loadplanning.infrastructure.messaging;

import java.math.BigDecimal;

import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadedUld;

/** One positioned ULD in a load-finalized event. */
record UldLineJson(String uldCode, String position, BigDecimal weight, String weightUnit, String destination) {

    static UldLineJson from(LoadedUld uld) {
        return new UldLineJson(uld.code().value(), uld.position().name(), uld.weight().value(),
            uld.weight().unit().name(), uld.destination().value());
    }
}
