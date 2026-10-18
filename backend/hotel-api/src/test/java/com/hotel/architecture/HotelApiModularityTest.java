package com.hotel.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.hotel.HotelApiApplication;
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
 * Architectural and modular boundary tests for {@code hotel-api} (AC-3).
 */
class HotelApiModularityTest {

    private static final Set<String> EXPECTED_BUSINESS_MODULES =
            Set.of(
                    "sharedkernel",
                    "catalog",
                    "pricing",
                    "media",
                    "booking",
                    "payment",
                    "notification",
                    "reporting");

    private static final Set<String> SCHEMA_OWNING_MODULES =
            Set.of("catalog", "pricing", "booking", "payment", "media", "notification");

    private static final Set<String> SCHEMALESS_MODULES = Set.of("sharedkernel", "reporting");

    /**
     * Closed allowlist of cross-module dependencies from {@code docs/architecture.md} §5 R3.
     * Every module may also depend on {@code sharedkernel}.
     */
    private static final Map<String, Set<String>> ALLOWED_MODULE_DEPENDENCIES =
            Map.of(
                    "sharedkernel", Set.of(),
                    "catalog", Set.of(),
                    "pricing", Set.of(),
                    "payment", Set.of(),
                    "media", Set.of("catalog"),
                    "booking", Set.of("catalog", "pricing", "payment"),
                    "notification", Set.of("booking", "payment"),
                    "reporting", Set.of("booking", "catalog", "payment", "pricing"));

    private static JavaClasses productionClasses;

    @BeforeAll
    static void importClasses() {
        productionClasses =
                new ClassFileImporter()
                        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                        .importPackages("com.hotel");
    }

    @Test
    void shouldVerifyModularStructure_whenCheckingEightBusinessModules() {
        ApplicationModules modules = ApplicationModules.of(HotelApiApplication.class);

        Set<String> detectedNames =
                modules.stream().map(ApplicationModule::getName).collect(Collectors.toSet());

        assertThat(detectedNames).isEqualTo(EXPECTED_BUSINESS_MODULES);
        assertThat(detectedNames)
                .doesNotContain("identity", "web", "config", "scaffold");

        modules.verify();
    }

    @Test
    void shouldDefineApiAndInternalSubpackages_whenInspectingAllBusinessModules() {
        Set<String> packageNames =
                productionClasses.stream()
                        .map(JavaClass::getPackageName)
                        .collect(Collectors.toSet());

        for (String module : EXPECTED_BUSINESS_MODULES) {
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
    void shouldForbidForbiddenModuleDependencies_whenCheckingClosedAllowlist() {
        // Explicit negative direction assertions (docs/architecture.md §5 R3)
        noClasses()
                .that()
                .resideInAPackage("com.hotel.catalog..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("com.hotel.booking..")
                .allowEmptyShould(true)
                .check(productionClasses);

        noClasses()
                .that()
                .resideInAPackage("com.hotel.payment..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("com.hotel.booking..")
                .allowEmptyShould(true)
                .check(productionClasses);

        noClasses()
                .that()
                .resideInAPackage("com.hotel.sharedkernel..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "com.hotel.catalog..",
                        "com.hotel.pricing..",
                        "com.hotel.media..",
                        "com.hotel.booking..",
                        "com.hotel.payment..",
                        "com.hotel.notification..",
                        "com.hotel.reporting..",
                        "com.hotel.web..",
                        "com.hotel.config..",
                        "com.hotel.scaffold..")
                .allowEmptyShould(true)
                .check(productionClasses);

        // Enforce complete closed allowlist for all 8 business modules + cross-cutting packages
        for (String sourceModule : EXPECTED_BUSINESS_MODULES) {
            Set<String> allowedTargets = ALLOWED_MODULE_DEPENDENCIES.get(sourceModule);

            for (String candidateTarget : EXPECTED_BUSINESS_MODULES) {
                if (candidateTarget.equals(sourceModule)
                        || candidateTarget.equals("sharedkernel")
                        || allowedTargets.contains(candidateTarget)) {
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

            // Business modules must never depend on root cross-cutting packages (one-way rule)
            noClasses()
                    .that()
                    .resideInAPackage("com.hotel." + sourceModule + "..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.hotel.web..", "com.hotel.config..", "com.hotel.scaffold..")
                    .allowEmptyShould(true)
                    .check(productionClasses);
        }
    }

    @Test
    void shouldForbidInternalPackageAccess_whenAccessedFromOtherModules() {
        for (String module : EXPECTED_BUSINESS_MODULES) {
            noClasses()
                    .that()
                    .resideOutsideOfPackage("com.hotel." + module + "..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("com.hotel." + module + ".internal..")
                    .allowEmptyShould(true)
                    .check(productionClasses);
        }
    }

    @Test
    void shouldForbidCyclicDependencies_whenCheckingModules() {
        SlicesRuleDefinition.slices()
                .matching("com.hotel.(*)..")
                .should()
                .beFreeOfCycles()
                .allowEmptyShould(true)
                .check(productionClasses);
    }

    @Test
    void shouldForbidEntityLeakage_whenCheckingApiAndWebPackages() {
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
    }

    @Test
    void shouldForbidDoubleAndFloat_whenCheckingProductionClasses() {
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
                                                        + " uses forbidden floating-point type "
                                                        + field.getRawType().getName()));
                            }
                        }
                        for (JavaMethod method : javaClass.getMethods()) {
                            if (isFloatingPoint(method.getRawReturnType())) {
                                events.add(
                                        SimpleConditionEvent.violated(
                                                method,
                                                "Method "
                                                        + method.getFullName()
                                                        + " returns forbidden floating-point type "
                                                        + method.getRawReturnType().getName()));
                            }
                            for (JavaClass paramType : method.getRawParameterTypes()) {
                                if (isFloatingPoint(paramType)) {
                                    events.add(
                                            SimpleConditionEvent.violated(
                                                    method,
                                                    "Method "
                                                            + method.getFullName()
                                                            + " has parameter of forbidden floating-point type "
                                                            + paramType.getName()));
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
    void shouldNotTouchOtherSchemas_whenInspectingModuleMigrations() throws IOException {
        Path migrationRoot = Path.of("src/main/resources/db/migration");

        for (String schemaless : SCHEMALESS_MODULES) {
            assertThat(Files.exists(migrationRoot.resolve(schemaless))).isFalse();
        }

        for (String module : SCHEMA_OWNING_MODULES) {
            Path moduleDir = migrationRoot.resolve(module);
            assertThat(Files.isDirectory(moduleDir)).isTrue();

            try (Stream<Path> files = Files.list(moduleDir)) {
                List<Path> sqlFiles =
                        files.filter(p -> p.toString().endsWith(".sql")).toList();
                assertThat(sqlFiles).isNotEmpty();

                for (Path sqlFile : sqlFiles) {
                    assertThat(sqlFile.getFileName().toString()).matches("^V\\d+__.*\\.sql$");
                    String content = Files.readString(sqlFile).toLowerCase();
                    for (String otherModule : SCHEMA_OWNING_MODULES) {
                        if (!otherModule.equals(module)) {
                            assertThat(content)
                                    .as(
                                            "Migration %s must not touch schema %s",
                                            sqlFile.getFileName(), otherModule)
                                    .doesNotContain("schema " + otherModule)
                                    .doesNotContain(otherModule + ".");
                        }
                    }
                }
            }
        }
    }
}
