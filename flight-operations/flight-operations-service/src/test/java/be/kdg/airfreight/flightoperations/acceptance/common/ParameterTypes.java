package be.kdg.airfreight.flightoperations.acceptance.common;

import be.kdg.airfreight.flightoperations.domain.flight.DelayKind;
import io.cucumber.java.ParameterType;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** The parameter types of both suites: the unit and the integration glue both read this package. */
public class ParameterTypes {
    private static final String MONTHS =
        "January|February|March|April|May|June|July|August|September|October|November|December";

    /** A UTC time as the stories write it: "06:45". */
    @ParameterType("\\d{2}:\\d{2}")
    public LocalTime time(String time) {
        return LocalTime.parse(time);
    }

    /** A date as the stories write it: "9 January 2026". */
    @ParameterType("(\\d{1,2}) (" + MONTHS + ") (\\d{4})")
    public LocalDate date(String day, String month, String year) {
        return LocalDate.of(Integer.parseInt(year), Month.valueOf(month.toUpperCase(Locale.ROOT)),
            Integer.parseInt(day));
    }

    /** A UTC moment as the stories write it: "16 January 2026 00:00". */
    @ParameterType("(\\d{1,2}) (" + MONTHS + ") (\\d{4}) (\\d{2}):(\\d{2})")
    public Instant moment(String day, String month, String year, String hour, String minute) {
        return LocalDateTime.of(Integer.parseInt(year), Month.valueOf(month.toUpperCase(Locale.ROOT)),
                Integer.parseInt(day), Integer.parseInt(hour), Integer.parseInt(minute))
            .toInstant(ZoneOffset.UTC);
    }

    @ParameterType(MONTHS)
    public Month month(String month) {
        return Month.valueOf(month.toUpperCase(Locale.ROOT));
    }

    /** "5 and 8", "5, 8 and 12": days of a month. */
    @ParameterType("\\d{1,2}(?:, \\d{1,2})*(?: and \\d{1,2})?")
    public List<Integer> days(String days) {
        return Arrays.stream(days.split(", | and ")).map(Integer::valueOf).toList();
    }

    /** "departure" or "arrival". */
    @ParameterType("departure|arrival")
    public DelayKind delayKind(String kind) {
        return DelayKind.valueOf(kind.toUpperCase(Locale.ROOT));
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
