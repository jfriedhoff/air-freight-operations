package be.kdg.airfreight.messaging.acceptance.unit;

import io.cucumber.java.ParameterType;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ParameterTypes {
    private static final String MONTHS =
        "January|February|March|April|May|June|July|August|September|October|November|December";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter MOMENT = DateTimeFormatter.ofPattern("d MMMM yyyy HH:mm", Locale.ENGLISH);
    private static final DateTimeFormatter TELEGRAM_TIME = DateTimeFormatter.ofPattern("HHmm");

    /** "06:30". */
    @ParameterType("\\d{2}:\\d{2}")
    public LocalTime time(String time) {
        return LocalTime.parse(time);
    }

    /** "09": a day of the month as a telegram writes it. */
    @ParameterType("\\d{2}")
    public Integer day(String day) {
        return Integer.valueOf(day);
    }

    /** "1200": a time as a telegram writes it. */
    @ParameterType("\\d{4}")
    public LocalTime hhmm(String time) {
        return LocalTime.parse(time, TELEGRAM_TIME);
    }

    /** "9 January 2026". */
    @ParameterType("\\d{1,2} (?:" + MONTHS + ") \\d{4}")
    public LocalDate date(String date) {
        return LocalDate.parse(date, DATE);
    }

    /** "9 January 2026 08:00", in UTC. */
    @ParameterType("\\d{1,2} (?:" + MONTHS + ") \\d{4} \\d{2}:\\d{2}")
    public Instant moment(String moment) {
        return LocalDateTime.parse(moment, MOMENT).toInstant(ZoneOffset.UTC);
    }

    @ParameterType("Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday")
    public DayOfWeek weekday(String weekday) {
        return DayOfWeek.valueOf(weekday.toUpperCase(Locale.ROOT));
    }

    @ParameterType(MONTHS)
    public Month month(String month) {
        return Month.valueOf(month.toUpperCase(Locale.ROOT));
    }

    /** "5, 8, 12 and 15". */
    @ParameterType("\\d+(?:, \\d+)*(?: and \\d+)?")
    public List<Integer> days(String days) {
        return Arrays.stream(days.split(", | and ")).map(Integer::valueOf).toList();
    }
}
