package be.kdg.airfreight.flightoperations;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import org.jmolecules.archunit.JMoleculesDddRules;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.scheduling.annotation.Scheduled;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@AnalyzeClasses(packages = "be.kdg.airfreight.flightoperations")
class DddArchTest {
    /** An inbound adapter is started by a message or by a timer, and is the only infrastructure that calls a use case. */
    private static final DescribedPredicate<JavaClass> HAVE_A_LISTENER_OR_SCHEDULED_METHOD =
        DescribedPredicate.describe("have a @RabbitListener or @Scheduled method",
            javaClass -> javaClass.getMethods().stream().anyMatch(method ->
                method.isAnnotatedWith(RabbitListener.class) || method.isAnnotatedWith(Scheduled.class)));
    private static final DescribedPredicate<JavaClass> ARE_INFRASTRUCTURE =
        resideInAPackage("..flightoperations.infrastructure..");

    @ArchTest
    void whenCheckingAllClasses_thenCodeFollowsAllDddPrinciples(JavaClasses classes) {
        JMoleculesDddRules.all().check(classes);
    }

    @ArchTest
    void whenCheckingAllClasses_thenDependenciesPointTowardsTheDomain(JavaClasses classes) {
        layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .withOptionalLayers(true)
            .layer("API").definedBy("..flightoperations.api..")
            .layer("Application").definedBy("..flightoperations.application..")
            .layer("Domain").definedBy("..flightoperations.domain..")
            .layer("Inbound adapters").definedBy(ARE_INFRASTRUCTURE.and(HAVE_A_LISTENER_OR_SCHEDULED_METHOD))
            .layer("Infrastructure").definedBy(ARE_INFRASTRUCTURE.and(not(HAVE_A_LISTENER_OR_SCHEDULED_METHOD)))
            .whereLayer("API").mayNotBeAccessedByAnyLayer()
            .whereLayer("Application").mayOnlyBeAccessedByLayers("API", "Inbound adapters")
            .whereLayer("Inbound adapters").mayNotBeAccessedByAnyLayer()
            .whereLayer("Infrastructure").mayOnlyBeAccessedByLayers("Inbound adapters")
            .check(classes);
    }

    @ArchTest
    void whenCheckingTheDomain_thenItDoesNotDependOnTheFramework(JavaClasses classes) {
        noClasses()
            .that().resideInAPackage("..flightoperations.domain..")
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta..")
            .check(classes);
    }
}
