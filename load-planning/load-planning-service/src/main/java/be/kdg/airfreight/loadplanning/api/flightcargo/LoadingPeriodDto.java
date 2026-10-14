package be.kdg.airfreight.loadplanning.api.flightcargo;

import java.time.Instant;

import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadingPeriod;

/** The start is included, the end is not. Both in UTC. */
public record LoadingPeriodDto(Instant start, Instant end) {
    public static LoadingPeriodDto from(LoadingPeriod period) {
        return new LoadingPeriodDto(period.start(), period.end());
    }
}
