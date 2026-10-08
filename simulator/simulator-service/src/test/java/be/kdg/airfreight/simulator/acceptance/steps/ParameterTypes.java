package be.kdg.airfreight.simulator.acceptance.steps;

import java.time.LocalDate;
import java.time.Month;
import java.util.Locale;

import io.cucumber.java.ParameterType;

public class ParameterTypes {
    /** A date as the stories write it: "9 January 2026". */
    @ParameterType("(\\d{1,2}) (January|February|March|April|May|June|July|August|September|October|November|December)"
        + " (\\d{4})")
    public LocalDate date(String day, String month, String year) {
        return LocalDate.of(Integer.parseInt(year), Month.valueOf(month.toUpperCase(Locale.ROOT)),
            Integer.parseInt(day));
    }
}
