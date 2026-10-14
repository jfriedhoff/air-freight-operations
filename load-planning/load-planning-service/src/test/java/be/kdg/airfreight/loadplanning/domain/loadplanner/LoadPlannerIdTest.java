package be.kdg.airfreight.loadplanning.domain.loadplanner;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoadPlannerIdTest {
    @Test
    void aLoadPlannerTakesTheIdentityOfItsAccount() {
        UUID account = UUID.randomUUID();

        assertThat(LoadPlannerId.fromAccount(account.toString())).isEqualTo(new LoadPlannerId(account));
    }

    @Test
    void anAccountWithoutAUuidIsNotALoadPlanner() {
        assertThatThrownBy(() -> LoadPlannerId.fromAccount("user")).isInstanceOf(UnknownLoadPlannerException.class);
        assertThatThrownBy(() -> LoadPlannerId.fromAccount(null)).isInstanceOf(UnknownLoadPlannerException.class);
    }

    @Test
    void anUnregisteredLoadPlannerIsRefusedWithAnExplanation() {
        assertThat(LoadPlannerId.create().notRegistered()).hasMessage("You are not registered as a load planner");
    }
}
