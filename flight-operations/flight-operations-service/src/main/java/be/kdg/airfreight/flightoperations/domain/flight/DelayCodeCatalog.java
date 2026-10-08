package be.kdg.airfreight.flightoperations.domain.flight;

import java.util.List;
import java.util.Optional;

import be.kdg.airfreight.flightoperations.domain.InvalidValueException;

/** The IATA delay-code list. Where the list comes from is infrastructure; which codes count is decided here. */
public interface DelayCodeCatalog {
    List<DelayReason> all();

    default Optional<DelayReason> find(DelayCode code) {
        return all().stream().filter(reason -> reason.code().equals(code)).findFirst();
    }

    default DelayReason requireKnown(DelayCode code) {
        return find(code).orElseThrow(() -> new InvalidValueException(
            "Delay reason '" + code.value() + "' is not in the IATA list"));
    }
}
