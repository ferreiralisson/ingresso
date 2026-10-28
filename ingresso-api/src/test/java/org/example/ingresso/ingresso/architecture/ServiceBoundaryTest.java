package org.example.ingresso.ingresso.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ServiceBoundaryTest {

    private static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages("org.example.ingresso.ingresso");

    @Test
    void controllersDependOnServiceContractsRatherThanImplementations() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..controller..")
            .should().dependOnClassesThat().resideInAPackage("..service.impl..");

        rule.check(PRODUCTION_CLASSES);
    }

    @Test
    void serviceInterfacesDoNotDependOnFrameworkOrInternalLayers() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("org.example.ingresso.ingresso.service")
            .and().areInterfaces()
            .should().dependOnClassesThat().resideInAnyPackage(
                "..controller..",
                "..model..",
                "..repository..",
                "..security..",
                "org.springframework.."
            );

        rule.check(PRODUCTION_CLASSES);
    }

    @Test
    void applicationDtosDoNotDependOnDomainOrPersistenceModels() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..service.dto..")
            .should().dependOnClassesThat().resideInAnyPackage("..model..", "..repository..");

        rule.check(PRODUCTION_CLASSES);
    }

    @Test
    void serviceImplementationsDoNotDependOnOtherImplementations() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..service.impl..")
            .should().dependOnClassesThat().resideInAPackage("..service.impl..");

        rule.check(PRODUCTION_CLASSES);
    }
}