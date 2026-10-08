package be.kdg.airfreight.flightoperations.acceptance.unit;

import be.kdg.airfreight.flightoperations.domain.flight.DelayKind;
import io.cucumber.java.ParameterType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.util.Locale;

public class ParameterTypes {
    /** A UTC time as the stories write it: "06:45". */
    @ParameterType("\\d{2}:\\d{2}")
    public LocalTime time(String time) {
        return LocalTime.parse(time);
    }

    /** A date as the stories write it: "9 January 2026". */
    @ParameterType("(\\d{1,2}) (January|February|March|April|May|June|July|August|September|October|November|December)"
        + " (\\d{4})")
    public LocalDate date(String day, String month, String year) {
        return LocalDate.of(Integer.parseInt(year), Month.valueOf(month.toUpperCase(Locale.ROOT)),
            Integer.parseInt(day));
    }

    /** "departure" or "arrival". */
    @ParameterType("departure|arrival")
    public DelayKind delayKind(String kind) {
        return DelayKind.valueOf(kind.toUpperCase(Locale.ROOT));
    }
}
