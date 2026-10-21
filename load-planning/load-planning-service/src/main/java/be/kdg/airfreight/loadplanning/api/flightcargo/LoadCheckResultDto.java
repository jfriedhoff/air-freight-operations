package be.kdg.airfreight.loadplanning.api.flightcargo;

import java.math.BigDecimal;
import java.util.List;

import be.kdg.airfreight.loadplanning.domain.flightcargo.CheckSeverity;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LateralBalanceDetails;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadCheckResult;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadedUld;
import be.kdg.airfreight.loadplanning.domain.flightcargo.Obstruction;
import be.kdg.airfreight.loadplanning.domain.flightcargo.OffloadingDetails;
import be.kdg.airfreight.loadplanning.domain.flightcargo.Side;
import org.jspecify.annotations.Nullable;

/**
 * One check of the report. A failed check carries its reason; a passed check has none. The offloading check also
 * names the ULDs that cannot leave and what is in their way, the lateral balance check the weight on each side:
 * the cargo page shows them on the deck. A check without such details has neither.
 */
public record LoadCheckResultDto(String checkName, CheckSeverity severity, boolean passed, @Nullable String reason,
                                 @Nullable OffloadingDto offloading, @Nullable LateralBalanceDto lateralBalance) {
    public record OffloadingDto(List<ObstructionDto> obstructions) {
    }

    public record ObstructionDto(UldOnDeckDto stuck, List<UldOnDeckDto> inTheWay) {
        static ObstructionDto from(Obstruction obstruction) {
            return new ObstructionDto(UldOnDeckDto.from(obstruction.stuck()),
                obstruction.inTheWay().stream().map(UldOnDeckDto::from).toList());
        }
    }

    public record UldOnDeckDto(String uld, String position) {
        static UldOnDeckDto from(LoadedUld uld) {
            return new UldOnDeckDto(uld.code().value(), uld.position().name());
        }
    }

    public record LateralBalanceDto(BigDecimal leftKilograms, BigDecimal rightKilograms,
                                    BigDecimal maximumOnTheHeaviestSide, @Nullable Side heavierSide) {
    }

    public static LoadCheckResultDto from(LoadCheckResult result) {
        OffloadingDto offloading = null;
        LateralBalanceDto lateralBalance = null;
        switch (result.details()) {
            case OffloadingDetails details -> offloading = new OffloadingDto(
                details.obstructions().stream().map(ObstructionDto::from).toList());
            case LateralBalanceDetails details -> lateralBalance = new LateralBalanceDto(details.leftKilograms(),
                details.rightKilograms(), details.maximumOnTheHeaviestSide(), details.heavierSide());
            case null -> {
                // a check without details: the result only has its reason
            }
        }
        return new LoadCheckResultDto(result.checkName(), result.severity(), result.passed(), result.reason(),
            offloading, lateralBalance);
    }
}
