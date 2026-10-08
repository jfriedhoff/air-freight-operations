package be.kdg.airfreight.messaging.acceptance.unit;

import io.cucumber.java.ParameterType;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.Month;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ParameterTypes {
    /** "06:30". */
    @ParameterType("\\d{2}:\\d{2}")
    public LocalTime time(String time) {
        return LocalTime.parse(time);
    }

    @ParameterType("Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday")
    public DayOfWeek weekday(String weekday) {
        return DayOfWeek.valueOf(weekday.toUpperCase(Locale.ROOT));
    }

    @ParameterType("January|February|March|April|May|June|July|August|September|October|November|December")
    public Month month(String month) {
        return Month.valueOf(month.toUpperCase(Locale.ROOT));
    }

    /** "5, 8, 12 and 15". */
    @ParameterType("\\d+(?:, \\d+)*(?: and \\d+)?")
    public List<Integer> days(String days) {
        return Arrays.stream(days.split(", | and ")).map(Integer::valueOf).toList();
    }
}
