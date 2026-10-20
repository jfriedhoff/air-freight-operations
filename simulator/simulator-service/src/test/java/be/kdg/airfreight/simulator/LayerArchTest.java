package be.kdg.airfreight.simulator;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@AnalyzeClasses(packages = "be.kdg.airfreight.simulator")
class LayerArchTest {
    /** No domain layer: the simulator is no bounded context and guards no rules; Messaging judges its messages. */
    @ArchTest
    void whenCheckingAllClasses_thenDependenciesPointTowardsTheApplication(JavaClasses classes) {
        layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .withOptionalLayers(true)
            .layer("API").definedBy("..simulator.api..")
            .layer("Application").definedBy("..simulator.application..")
            .layer("Infrastructure").definedBy("..simulator.infrastructure..")
            .whereLayer("API").mayNotBeAccessedByAnyLayer()
            .whereLayer("Application").mayOnlyBeAccessedByLayers("API", "Infrastructure")
            .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()
            .check(classes);
    }
}
