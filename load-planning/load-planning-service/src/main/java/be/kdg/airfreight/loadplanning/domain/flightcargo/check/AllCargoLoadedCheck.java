package be.kdg.airfreight.loadplanning.domain.flightcargo.check;

import java.util.List;

import be.kdg.airfreight.loadplanning.domain.flightcargo.CheckSeverity;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargo;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FloorPlan;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadCheck;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadCheckResult;

/** Booked cargo without a load position stays behind: a warning, not a reason to stop. */
public final class AllCargoLoadedCheck implements LoadCheck {
    private static final String NAME = "Not all cargo loaded";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public CheckSeverity severity() {
        return CheckSeverity.SOFT;
    }

    @Override
    public LoadCheckResult check(FlightCargo cargo, FloorPlan floorPlan) {
        List<String> withoutPosition = cargo.getUlds().stream()
            .filter(uld -> !uld.hasPosition())
            .map(uld -> uld.getCode().value() + " has no load position")
            .sorted()
            .toList();
        if (withoutPosition.isEmpty()) {
            return LoadCheckResult.pass(NAME, severity());
        }
        return LoadCheckResult.fail(NAME, severity(), String.join("; ", withoutPosition));
    }
}
