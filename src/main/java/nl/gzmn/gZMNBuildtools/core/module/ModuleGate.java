package nl.gzmn.gZMNBuildtools.core.module;

import java.util.Set;

/**
 * Runtime gate for module commands. Paper registers commands only once at
 * startup, so a module switched off at runtime keeps its commands registered;
 * they check this gate in their {@code requires} and refuse to run while the
 * module is not running. A null gate (no ModuleManager yet, e.g. in tests)
 * allows everything.
 */
public final class ModuleGate {

    private static volatile Set<String> running;

    private ModuleGate() {
    }

    static void setRunning(Set<String> ids) {
        running = Set.copyOf(ids);
    }

    /** Whether the module's commands may run right now. */
    public static boolean allows(String moduleId) {
        Set<String> current = running;
        return current == null || current.contains(moduleId);
    }
}
