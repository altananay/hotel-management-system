package com.hotel.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.hotel.AdminApiApplication;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import jakarta.persistence.Entity;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Architectural and modular boundary tests for {@code admin-api} (AC-5, CR-8, CR-10).
 */
class AdminApiModularityTest {

    private static final Set<String> EXPECTED_ADMIN_MODULES =
            Set.of("adminidentity", "adminaudit", "preferences", "gateway");

    /**
     * Closed allowlist of cross-module dependencies for admin modules (docs/architecture.md §4).
     * The list is intentionally EMPTY: no cross-module dependency has been approved yet. Any new
     * edge requires updating {@code docs/architecture.md} §4 first.
     */
    private static final Map<String, Set<String>> ALLOWED_ADMIN_MODULE_DEPENDENCIES =
            Map.of(
                    "adminidentity", Set.of(),
                    "adminaudit", Set.of(),
                    "preferences", Set.of(),
                    "gateway", Set.of());

    private static JavaClasses productionClasses;

    @BeforeAll
    static void importClasses() {
        productionClasses =
                new ClassFileImporter()
                        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                        .importPackages("com.hotel");
    }

    @Test
    void shouldVerifyModularStructure_whenCheckingFourAdminModules() {
        ApplicationModules modules = ApplicationModules.of(AdminApiApplication.class);

        Set<String> detectedNames =
                modules.stream().map(ApplicationModule::getName).collect(Collectors.toSet());

        assertThat(detectedNames).isEqualTo(EXPECTED_ADMIN_MODULES);
        assertThat(detectedNames).doesNotContain("web", "config", "scaffold");

        modules.verify();
    }

    @Test
    void shouldDefineApiAndInternalSubpackages_whenInspectingAllAdminModules() {
        Set<String> packageNames =
                productionClasses.stream()
                        .map(JavaClass::getPackageName)
                        .collect(Collectors.toSet());

        for (String module : EXPECTED_ADMIN_MODULES) {
            String base = "com.hotel." + module;
            assertThat(packageNames)
                    .contains(
                            base + ".api",
                            base + ".internal.domain",
                            base + ".internal.application",
                            base + ".internal.web",
                            base + ".internal.persistence");
        }
    }

    @Test
    void shouldForbidCrossModuleDependencies_whenCheckingClosedAllowlist() {
        for (String sourceModule : EXPECTED_ADMIN_MODULES) {
            Set<String> allowedTargets = ALLOWED_ADMIN_MODULE_DEPENDENCIES.get(sourceModule);

            for (String candidateTarget : EXPECTED_ADMIN_MODULES) {
                if (candidateTarget.equals(sourceModule) || allowedTargets.contains(candidateTarget)) {
                    continue;
                }
                noClasses()
                        .that()
                        .resideInAPackage("com.hotel." + sourceModule + "..")
                        .should()
                        .dependOnClassesThat()
                        .resideInAPackage("com.hotel." + candidateTarget + "..")
                        .allowEmptyShould(true)
                        .check(productionClasses);
            }
        }

        // Explicit negative direction example required by the review: adminaudit must not depend on
        // adminidentity while the allowlist stays empty (docs/architecture.md §4).
        noClasses()
                .that()
                .resideInAPackage("com.hotel.adminaudit..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("com.hotel.adminidentity..")
                .allowEmptyShould(true)
                .check(productionClasses);
    }

    @Test
    void shouldForbidInternalPackageAccessAndCrossCuttingReverseDependencies_whenCheckingBoundaries() {
        for (String module : EXPECTED_ADMIN_MODULES) {
            noClasses()
                    .that()
                    .resideOutsideOfPackage("com.hotel." + module + "..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("com.hotel." + module + ".internal..")
                    .allowEmptyShould(true)
                    .check(productionClasses);

            noClasses()
                    .that()
                    .resideInAPackage("com.hotel." + module + "..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.hotel.web..", "com.hotel.config..", "com.hotel.scaffold..")
                    .allowEmptyShould(true)
                    .check(productionClasses);
        }

        SlicesRuleDefinition.slices()
                .matching("com.hotel.(*)..")
                .should()
                .beFreeOfCycles()
                .allowEmptyShould(true)
                .check(productionClasses);
    }

    @Test
    void shouldForbidEntityLeakageAndDoubleFloat_whenCheckingProductionClasses() {
        DescribedPredicate<JavaClass> isJpaEntity =
                new DescribedPredicate<>("annotated with @Entity") {
                    @Override
                    public boolean test(JavaClass input) {
                        return input.isAnnotatedWith(Entity.class);
                    }
                };

        noClasses()
                .that()
                .resideInAnyPackage(
                        "..api..", "..internal.web..", "com.hotel.web..", "com.hotel.scaffold..")
                .should()
                .beAnnotatedWith(Entity.class)
                .orShould()
                .dependOnClassesThat(isJpaEntity)
                .allowEmptyShould(true)
                .check(productionClasses);

        ArchCondition<JavaClass> notUseDoubleOrFloat =
                new ArchCondition<>("not declare fields, parameters, or return types of double/float") {
                    @Override
                    public void check(JavaClass javaClass, ConditionEvents events) {
                        for (JavaField field : javaClass.getFields()) {
                            if (isFloatingPoint(field.getRawType())) {
                                events.add(
                                        SimpleConditionEvent.violated(
                                                field,
                                                "Field "
                                                        + field.getFullName()
                                                        + " uses forbidden floating-point type"));
                            }
                        }
                        for (JavaMethod method : javaClass.getMethods()) {
                            if (isFloatingPoint(method.getRawReturnType())) {
                                events.add(
                                        SimpleConditionEvent.violated(
                                                method,
                                                "Method "
                                                        + method.getFullName()
                                                        + " returns forbidden floating-point type"));
                            }
                            for (JavaClass paramType : method.getRawParameterTypes()) {
                                if (isFloatingPoint(paramType)) {
                                    events.add(
                                            SimpleConditionEvent.violated(
                                                    method,
                                                    "Method "
                                                            + method.getFullName()
                                                            + " has parameter of forbidden floating-point type"));
                                }
                            }
                        }
                    }

                    private boolean isFloatingPoint(JavaClass type) {
                        String name = type.getName();
                        return name.equals("double")
                                || name.equals("float")
                                || name.equals(Double.class.getName())
                                || name.equals(Float.class.getName());
                    }
                };

        ArchRuleDefinition.classes()
                .that()
                .resideInAPackage("com.hotel..")
                .should(notUseDoubleOrFloat)
                .check(productionClasses);
    }

    @Test
    void shouldNotReferenceBusinessCoreDatabase_whenScanningAllAdminApiMainSources()
            throws IOException {
        Path sourceRoot = Path.of("src/main");
        List<Path> files;
        try (Stream<Path> stream = Files.walk(sourceRoot)) {
            files =
                    stream.filter(Files::isRegularFile)
                            .filter(
                                    path -> {
                                        String name = path.getFileName().toString();
                                        return name.endsWith(".java")
                                                || name.endsWith(".yml")
                                                || name.endsWith(".yaml")
                                                || name.endsWith(".properties");
                                    })
                            .toList();
        }

        // pom.xml de üretim yüzeyidir (Flyway/datasource property'leri); taramaya dahildir.
        List<Path> filesToScan =
                Stream.concat(files.stream(), Stream.of(Path.of("pom.xml"))).toList();

        assertThat(filesToScan).isNotEmpty();

        for (Path file : filesToScan) {
            assertThat(Files.readString(file))
                    .as(
                            "admin-api must not reference the business core database (checked: %s)",
                            file)
                    .doesNotContain("hotel_core");
        }
    }
}
