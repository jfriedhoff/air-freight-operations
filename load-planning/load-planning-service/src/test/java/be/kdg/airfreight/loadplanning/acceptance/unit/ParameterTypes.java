package be.kdg.airfreight.loadplanning.acceptance.unit;

import io.cucumber.java.ParameterType;

public class ParameterTypes {
    /** A weight as the stories write it: "1000 kg", "2646 lb", "1200.2 kg". */
    @ParameterType("\\d+(?:\\.\\d+)? (?:kg|lb)")
    public String weight(String weight) {
        return weight;
    }
}
