package be.kdg.airfreight.flightoperations.api.flight;

import be.kdg.airfreight.flightoperations.domain.flight.AirportCatalog;
import be.kdg.airfreight.flightoperations.domain.flight.AirportCode;
import org.jspecify.annotations.Nullable;

/** An airport as IATA code and name; the name is missing when the IATA list does not have the code. */
public record AirportDto(String code, @Nullable String name) {
    public static AirportDto from(AirportCode code, AirportCatalog airports) {
        return new AirportDto(code.value(), airports.nameOf(code).orElse(null));
    }
}
