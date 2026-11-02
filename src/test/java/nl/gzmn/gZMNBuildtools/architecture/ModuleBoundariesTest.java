package nl.gzmn.gZMNBuildtools.architecture;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import nl.gzmn.gZMNBuildtools.GZMNBuildtools;
import nl.gzmn.gZMNBuildtools.core.module.GzmnModule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The module rules, checked against the compiled classes (like Spring
 * Modulith's verify):
 * <ul>
 *   <li>the kernel (everything outside {@code modules}) never depends on a module;</li>
 *   <li>a module only uses modules it declares as dependencies;</li>
 *   <li>and only through their top-level package (their API), never their internals;</li>
 *   <li>modules do not depend on each other in cycles.</li>
 * </ul>
 */
class ModuleBoundariesTest {

    private static final String BASE = "nl.gzmn.gZMNBuildtools";
    private static final String MODULES = BASE + ".modules";

    private static JavaClasses classes;
    private static Map<String, GzmnModule> modulesByPackage;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE);
        modulesByPackage = new HashMap<>();
        for (GzmnModule module : GZMNBuildtools.allModules()) {
            modulesByPackage.put(module.getClass().getPackageName(), module);
        }
    }

    /** The module package a class belongs to, e.g. ...modules.splines for ...modules.splines.command.X. */
    private static String modulePackage(String packageName) {
        if (!packageName.startsWith(MODULES + ".")) {
            return null;
        }
        int end = packageName.indexOf('.', MODULES.length() + 1);
        return end < 0 ? packageName : packageName.substring(0, end);
    }

    @Test
    void everyModulePackageHasItsModuleClassRegistered() {
        Set<String> packages = classes.stream()
                .map(c -> modulePackage(c.getPackageName()))
                .filter(p -> p != null)
                .collect(Collectors.toCollection(TreeSet::new));
        assertEquals(packages, new TreeSet<>(modulesByPackage.keySet()),
                "each package under modules needs exactly one GzmnModule listed in GZMNBuildtools.allModules()");
        for (String pkg : packages) {
            long moduleClasses = classes.stream()
                    .filter(c -> c.getPackageName().equals(pkg) && c.isAssignableTo(GzmnModule.class))
                    .count();
            assertEquals(1, moduleClasses, pkg + " should contain one GzmnModule");
        }
    }

    @Test
    void theKernelDoesNotDependOnModules() {
        noClasses().that().resideOutsideOfPackage(MODULES + "..")
                .and().haveNameNotMatching(BASE.replace(".", "\\.") + "\\.GZMNBuildtools(\\$.*)?")
                .should().dependOnClassesThat().resideInAPackage(MODULES + "..")
                .because("the kernel is shared by all modules and must work with any of them switched off")
                .check(classes);
    }

    @Test
    void modulesUseOnlyDeclaredDependenciesThroughTheirApi() {
        List<String> violations = new ArrayList<>();
        for (JavaClass origin : classes) {
            String from = modulePackage(origin.getPackageName());
            if (from == null) {
                continue;
            }
            GzmnModule module = modulesByPackage.get(from);
            for (Dependency dependency : origin.getDirectDependenciesFromSelf()) {
                JavaClass target = dependency.getTargetClass();
                String to = modulePackage(target.getPackageName());
                if (to == null || to.equals(from)) {
                    continue;
                }
                GzmnModule used = modulesByPackage.get(to);
                if (used == null || !module.dependencies().contains(used.id())) {
                    violations.add(origin.getName() + " uses module '" + (used == null ? to : used.id())
                            + "' without declaring it: " + dependency.getDescription());
                } else if (!target.getPackageName().equals(to)) {
                    violations.add(origin.getName() + " uses internals of '" + used.id() + "': "
                            + dependency.getDescription());
                }
            }
        }
        assertTrue(violations.isEmpty(), String.join("\n", violations));
    }

    @Test
    void modulesAreFreeOfCycles() {
        slices().matching(MODULES + ".(*)..")
                .should().beFreeOfCycles()
                .check(classes);
    }
}
