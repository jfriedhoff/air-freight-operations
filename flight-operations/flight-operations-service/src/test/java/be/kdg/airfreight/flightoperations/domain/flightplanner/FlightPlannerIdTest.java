package be.kdg.airfreight.flightoperations.domain.flightplanner;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlightPlannerIdTest {
    @Test
    void aFlightPlannerTakesTheIdentityOfItsAccount() {
        UUID account = UUID.randomUUID();

        assertThat(FlightPlannerId.fromAccount(account.toString())).isEqualTo(new FlightPlannerId(account));
    }

    @Test
    void anAccountWithoutAUuidIsNotAFlightPlanner() {
        assertThatThrownBy(() -> FlightPlannerId.fromAccount("user"))
            .isInstanceOf(UnknownFlightPlannerException.class);
        assertThatThrownBy(() -> FlightPlannerId.fromAccount(null))
            .isInstanceOf(UnknownFlightPlannerException.class);
    }

    @Test
    void anUnregisteredFlightPlannerIsRefusedWithAnExplanation() {
        assertThat(FlightPlannerId.create().notRegistered())
            .hasMessage("You are not registered as a flight planner");
    }
}
