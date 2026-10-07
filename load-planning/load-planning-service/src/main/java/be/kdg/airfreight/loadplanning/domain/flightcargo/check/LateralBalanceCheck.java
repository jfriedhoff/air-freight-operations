package be.kdg.airfreight.loadplanning.domain.flightcargo.check;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import be.kdg.airfreight.loadplanning.domain.flightcargo.CheckSeverity;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FlightCargo;
import be.kdg.airfreight.loadplanning.domain.flightcargo.FloorPlan;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LateralBalanceLimit;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadCheck;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadCheckResult;
import be.kdg.airfreight.loadplanning.domain.flightcargo.Side;

/** The heaviest side may weigh at most the lightest side plus the limit. Positions without a side do not count. */
public final class LateralBalanceCheck implements LoadCheck {
    private static final String NAME = "Lateral balance";

    private final LateralBalanceLimit limit;

    public LateralBalanceCheck(LateralBalanceLimit limit) {
        this.limit = Objects.requireNonNull(limit, "Limit cannot be null");
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public CheckSeverity severity() {
        return CheckSeverity.HARD;
    }

    @Override
    public LoadCheckResult check(FlightCargo cargo, FloorPlan floorPlan) {
        BigDecimal left = kilogramsOn(cargo, Side.LEFT);
        BigDecimal right = kilogramsOn(cargo, Side.RIGHT);
        BigDecimal allowed = left.min(right).multiply(BigDecimal.ONE.add(limit.fraction()));
        if (left.max(right).compareTo(allowed) <= 0) {
            return LoadCheckResult.pass(NAME, severity());
        }
        return LoadCheckResult.fail(NAME, severity(), "Left carries " + plain(left) + " kg and right "
            + plain(right) + " kg; the heaviest side may carry at most " + plain(allowed) + " kg");
    }

    private static BigDecimal kilogramsOn(FlightCargo cargo, Side side) {
        return cargo.getLoad().stream()
            .filter(uld -> uld.isOnSide(side))
            .map(uld -> uld.getWeight().toKilograms())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Rounded to 0.1 kg for the report only; the check itself compares the exact values. */
    private static String plain(BigDecimal kilograms) {
        return kilograms.setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
