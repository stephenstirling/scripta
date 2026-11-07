package scripta.software.common.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Module dependency-direction guardrails. Allowed direction: {@code saas → proprietary → common}
 * and {@code scripta → proprietary → common}.
 */
class ArchitectureTest {

    private static final JavaClasses commonClasses =
            new ClassFileImporter()
                    .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_JARS)
                    .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                    .importPackages("scripta.software.common");

    @Test
    void commonDoesNotDependOnCore() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage("scripta.software.common..")
                        .should()
                        .dependOnClassesThat()
                        .resideInAPackage("scripta.software.core..");
        rule.check(commonClasses);
    }

    @Test
    void commonDoesNotDependOnProprietary() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage("scripta.software.common..")
                        .should()
                        .dependOnClassesThat()
                        .resideInAPackage("scripta.software.proprietary..");
        rule.check(commonClasses);
    }

    @Test
    void commonDoesNotDependOnSaas() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage("scripta.software.common..")
                        .should()
                        .dependOnClassesThat()
                        .resideInAPackage("scripta.software.saas..");
        rule.check(commonClasses);
    }

    @Test
    void clusterInterfacesHaveNoImplementationDependencies() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage("scripta.software.common.cluster..")
                        .should()
                        .dependOnClassesThat()
                        .resideInAnyPackage(
                                "scripta.software.proprietary..", "scripta.software.saas..");
        rule.check(commonClasses);
    }
}
