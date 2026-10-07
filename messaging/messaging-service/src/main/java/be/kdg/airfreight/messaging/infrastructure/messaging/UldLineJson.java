package be.kdg.airfreight.messaging.infrastructure.messaging;

import java.math.BigDecimal;

import be.kdg.airfreight.messaging.domain.load.CpmLine;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** One positioned ULD, in a load-finalized event and in a CPM. */
@JsonIgnoreProperties(ignoreUnknown = true)
record UldLineJson(String uldCode, String position, BigDecimal weight, String weightUnit, String destination) {

    static UldLineJson from(CpmLine line) {
        return new UldLineJson(line.uldCode(), line.position(), line.weight(), line.weightUnit(),
            line.destination().value());
    }
}
