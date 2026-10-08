package be.kdg.airfreight.loadplanning.acceptance.unit;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads a load as the offloading stories write it, such as "ATH on RR and UR, FRA on UL",
 * "ATH without load position, FRA on RL" or "ATH on A1, A2 and B1, CL and CR free".
 */
final class LoadDescription {
    private static final Pattern WITHOUT_POSITION = Pattern.compile("([A-Z]{3}) without load position");
    private static final Pattern ON_POSITIONS = Pattern.compile("([A-Z]{3}) on (.+)");

    /** One ULD: where it goes and where it is, or a null position when it has none. */
    record Uld(String destination, @Nullable String position) {
    }

    private LoadDescription() {
    }

    /**
     * Every part between commas is one of: "ATH without load position" (one ULD for ATH without a position),
     * "ATH on RR and UR" (ULDs for ATH), "A2 and B1" (more ULDs for the same destination) or "CL and CR free"
     * (nothing on them).
     */
    static List<Uld> parse(String description) {
        List<Uld> ulds = new ArrayList<>();
        String destination = "";
        for (String part : description.split(", ")) {
            Matcher without = WITHOUT_POSITION.matcher(part);
            Matcher on = ON_POSITIONS.matcher(part);
            if (without.matches()) {
                ulds.add(new Uld(without.group(1), null));
            } else if (on.matches()) {
                destination = on.group(1);
                ulds.addAll(on(destination, on.group(2)));
            } else if (!part.endsWith(" free")) {
                ulds.addAll(on(destination, part));
            }
        }
        return ulds;
    }

    /** "RR and UR" for ATH: a ULD for ATH on RR and one on UR. */
    private static List<Uld> on(String destination, String positions) {
        return Arrays.stream(positions.split(" and "))
            .map(position -> new Uld(destination, position))
            .toList();
    }
}
