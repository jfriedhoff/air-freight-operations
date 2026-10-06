package be.kdg.airfreight.loadplanning;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import org.jmolecules.archunit.JMoleculesDddRules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@AnalyzeClasses(packages = "be.kdg.airfreight.loadplanning")
class DddArchTest {
    @ArchTest
    void whenCheckingAllClasses_thenCodeFollowsAllDddPrinciples(JavaClasses classes) {
        JMoleculesDddRules.all().check(classes);
    }

    @ArchTest
    void whenCheckingAllClasses_thenDependenciesPointTowardsTheDomain(JavaClasses classes) {
        layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .withOptionalLayers(true)
            .layer("API").definedBy("..loadplanning.api..")
            .layer("Application").definedBy("..loadplanning.application..")
            .layer("Domain").definedBy("..loadplanning.domain..")
            .layer("Infrastructure").definedBy("..loadplanning.infrastructure..")
            .whereLayer("API").mayNotBeAccessedByAnyLayer()
            // Message listeners and schedulers are in infrastructure and call the application layer.
            .whereLayer("Application").mayOnlyBeAccessedByLayers("API", "Infrastructure")
            .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()
            .check(classes);
    }

    @ArchTest
    void whenCheckingTheDomain_thenItDoesNotDependOnTheFramework(JavaClasses classes) {
        noClasses()
            .that().resideInAPackage("..loadplanning.domain..")
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta..")
            .check(classes);
    }
}
