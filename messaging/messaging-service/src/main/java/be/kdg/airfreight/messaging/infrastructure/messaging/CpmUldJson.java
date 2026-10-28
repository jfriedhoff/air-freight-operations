package be.kdg.airfreight.messaging.infrastructure.messaging;

import java.math.BigDecimal;

import be.kdg.airfreight.messaging.domain.load.CpmLine;

/**
 * One ULD in a CPM for the partners. The weight unit is the IATA code: KGM for kilogram, LBR for pound.
 * Separate from UldLineJson, which is the contract with Load Planning and keeps its own field names.
 */
record CpmUldJson(String code, String position, Weight weight, String destination) {

    record Weight(BigDecimal amount, String unit) {
    }

    static CpmUldJson from(CpmLine line) {
        return new CpmUldJson(line.uldCode(), line.position(),
            new Weight(line.weight(), iataUnit(line.weightUnit())), line.destination().value());
    }

    private static String iataUnit(String unit) {
        return switch (unit) {
            case "KG" -> "KGM";
            case "LB" -> "LBR";
            default -> throw new IllegalArgumentException("Unknown weight unit " + unit);
        };
    }
}
