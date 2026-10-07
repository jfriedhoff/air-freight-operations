package be.kdg.airfreight.loadplanning.infrastructure.config;

import java.time.Clock;

import be.kdg.airfreight.loadplanning.domain.flightcargo.AircraftType;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LateralBalanceLimit;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadCheck;
import be.kdg.airfreight.loadplanning.domain.flightcargo.LoadingPolicy;
import be.kdg.airfreight.loadplanning.domain.flightcargo.check.AllCargoLoadedCheck;
import be.kdg.airfreight.loadplanning.domain.flightcargo.check.LateralBalanceCheck;
import be.kdg.airfreight.loadplanning.domain.flightcargo.check.OffloadingCheck;
import be.kdg.airfreight.loadplanning.infrastructure.floorplan.Boeing747400FFloorPlan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Hands the configured policy values to the domain, and makes its services beans. */
@Configuration
@EnableConfigurationProperties(PolicyProperties.class)
class PolicyConfig {
    @Bean
    LoadingPolicy loadingPolicy(PolicyProperties policy) {
        return new LoadingPolicy(policy.loadingOpensBeforeDeparture(), policy.loadingClosesBeforeDeparture());
    }

    /** The load checks, in the order the report shows them. A new check is one more bean here. */
    @Bean
    LoadCheck lateralBalanceCheck(PolicyProperties policy) {
        return new LateralBalanceCheck(new LateralBalanceLimit(policy.lateralBalanceLimit()));
    }

    @Bean
    LoadCheck offloadingCheck() {
        return new OffloadingCheck();
    }

    @Bean
    LoadCheck allCargoLoadedCheck() {
        return new AllCargoLoadedCheck();
    }

    /** The first version knows one aircraft type: every flight gets it. */
    @Bean
    AircraftType aircraftType() {
        return Boeing747400FFloorPlan.AIRCRAFT_TYPE;
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
