package be.kdg.airfreight.loadplanning.acceptance.unit;

import io.cucumber.java.en.But;
import io.cucumber.java.en.Given;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Finalizing the load. */
public class FinalizeLoadSteps {
    private static final String FINALIZED = "finalized and can no longer change";

    private final CargoWorld world;

    public FinalizeLoadSteps(CargoWorld world) {
        this.world = world;
    }

    /** Without load checks: they are not what these scenarios test, and a ULD on CL alone fails the lateral balance. */
    @Given("the load has been finalized")
    public void theLoadHasBeenFinalized() {
        world.finalizeLoad(List.of());
        world.expectedRefusal = FINALIZED;
    }

    @But("the load can still be finalized if all hard checks pass")
    public void theLoadCanStillBeFinalized() {
        assertThat(world.report.hardChecksPassed()).isTrue();
        world.finalizeLoad(CargoWorld.CHECKS);
        assertThat(world.cargo.isFinalized()).isTrue();
    }
}
