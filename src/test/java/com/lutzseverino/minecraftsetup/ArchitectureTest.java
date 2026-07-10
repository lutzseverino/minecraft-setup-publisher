package com.lutzseverino.minecraftsetup;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
        packages = "com.lutzseverino.minecraftsetup",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class ArchitectureTest {
    @ArchTest
    static final ArchRule DOMAIN_IS_INWARD_ONLY = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..application..", "..config..", "..infrastructure..", "..platform..", "..bootstrap.."
            );

    @ArchTest
    static final ArchRule APPLICATION_OWNS_PORTS = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..config..", "..infrastructure..", "..platform..", "..bootstrap.."
            );

    @ArchTest
    static final ArchRule ADAPTERS_DO_NOT_DEPEND_ON_BOOTSTRAP = noClasses()
            .that().resideInAnyPackage("..infrastructure..", "..platform..")
            .should().dependOnClassesThat().resideInAPackage("..bootstrap..");
}
