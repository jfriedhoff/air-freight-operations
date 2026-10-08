package be.kdg.airfreight.loadplanning.acceptance.unit;

import be.kdg.airfreight.loadplanning.domain.flightcargo.UldCode;
import io.cucumber.java.ParameterType;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.Locale;

public class ParameterTypes {
    /** A ULD code as the stories write it: "AKE16689R7". */
    @ParameterType("[A-Z]{3}\\d{4,5}[A-Z0-9]{2}")
    public UldCode uld(String code) {
        return new UldCode(code);
    }

    /** A weight as the stories write it: "1000 kg", "2646 lb", "1200.2 kg". */
    @ParameterType("\\d+(?:\\.\\d+)? (?:kg|lb)")
    public String weight(String weight) {
        return weight;
    }

    /** A UTC moment as the stories write it: "9 January 2026 06:30" or, in 2026, "8 January 18:29". */
    @ParameterType("(\\d{1,2}) (January|February|March|April|May|June|July|August|September|October|November|December)"
        + "(?: (\\d{4}))? (\\d{2}):(\\d{2})")
    public Instant moment(String day, String month, String year, String hour, String minute) {
        return LocalDateTime.of(year == null ? 2026 : Integer.parseInt(year),
                Month.valueOf(month.toUpperCase(Locale.ROOT)), Integer.parseInt(day),
                Integer.parseInt(hour), Integer.parseInt(minute))
            .toInstant(ZoneOffset.UTC);
    }
}
