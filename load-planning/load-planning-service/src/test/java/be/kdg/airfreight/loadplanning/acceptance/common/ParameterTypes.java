package be.kdg.airfreight.loadplanning.acceptance.common;

import be.kdg.airfreight.loadplanning.domain.flightcargo.UldCode;
import io.cucumber.java.ParameterType;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.Locale;

/** The parameter types of both suites: the unit and the integration glue both read this package. */
public class ParameterTypes {
    private static final String MONTHS =
        "January|February|March|April|May|June|July|August|September|October|November|December";

    /** A ULD code as the stories write it: "AKE16689R7". */
    @ParameterType("[A-Z]{3}\\d{4,5}[A-Z0-9]{2}")
    public UldCode uld(String code) {
        return new UldCode(code);
    }

    /** A weight as the stories write it: "1000 kg", "2646 lb", "1200.2 kg", and "-1 kg" for a refusal. */
    @ParameterType("-?\\d+(?:\\.\\d+)? (?:kg|lb)")
    public String weight(String weight) {
        return weight;
    }

    /** "06:45". */
    @ParameterType("\\d{2}:\\d{2}")
    public LocalTime time(String time) {
        return LocalTime.parse(time);
    }

    /** "09": a day of the month as a ULD message writes it. */
    @ParameterType("\\d{2}")
    public Integer day(String day) {
        return Integer.valueOf(day);
    }

    /** A date as the stories write it: "9 January 2026" or, in 2026, "9 January". */
    @ParameterType("(\\d{1,2}) (" + MONTHS + ")(?: (\\d{4}))?")
    public LocalDate date(String day, String month, String year) {
        return LocalDate.of(year == null ? 2026 : Integer.parseInt(year),
            Month.valueOf(month.toUpperCase(Locale.ROOT)), Integer.parseInt(day));
    }

    /** A UTC moment as the stories write it: "9 January 2026 06:30" or, in 2026, "8 January 18:29". */
    @ParameterType("(\\d{1,2}) (" + MONTHS + ")(?: (\\d{4}))? (\\d{2}):(\\d{2})")
    public Instant moment(String day, String month, String year, String hour, String minute) {
        return LocalDateTime.of(year == null ? 2026 : Integer.parseInt(year),
                Month.valueOf(month.toUpperCase(Locale.ROOT)), Integer.parseInt(day),
                Integer.parseInt(hour), Integer.parseInt(minute))
            .toInstant(ZoneOffset.UTC);
    }

    /** "400 Bad Request": the code and its reason phrase must belong together, so a typo cannot make a test pass. */
    @ParameterType("(\\d{3}) ([A-Z][A-Za-z]*(?: [A-Z][A-Za-z]*)*)")
    public HttpStatus status(String code, String reasonPhrase) {
        HttpStatus status = HttpStatus.valueOf(Integer.parseInt(code));
        if (!status.getReasonPhrase().equals(reasonPhrase)) {
            throw new IllegalArgumentException(code + " is " + status.getReasonPhrase() + ", not " + reasonPhrase);
        }
        return status;
    }
}
