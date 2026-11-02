package nl.gzmn.gZMNBuildtools.core.module;

import java.util.List;

/**
 * One feature of the plugin (a brush, a command family, the web editor, ...).
 * A module registers its own commands, listeners and services when enabled,
 * may only use the modules it lists as {@link #dependencies()}, and can be
 * switched off in {@code config.yml} under {@code modules.<id>}.
 */
public interface GzmnModule {

    /** Stable id, used as the config key ({@code modules.<id>}). */
    String id();

    /** One line describing what the module adds, shown by /gzmnbuildtools modules. */
    String description();

    /** Ids of modules this one needs; they are enabled first. */
    default List<String> dependencies() {
        return List.of();
    }

    /**
     * Starts the module. Throwing disables just this module (and modules that
     * depend on it); throw {@link ModuleDisabledException} to switch it off on purpose.
     */
    void enable(ModuleContext context) throws Exception;

    /** Re-reads configuration after /gzmnbuildtools reload. */
    default void reload(ModuleContext context) {
    }

    /** Releases resources (threads, ports, files) on shutdown. */
    default void disable() {
    }
}
