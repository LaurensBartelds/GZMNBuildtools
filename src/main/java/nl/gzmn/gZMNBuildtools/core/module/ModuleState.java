package nl.gzmn.gZMNBuildtools.core.module;

/** Where a module ended up after startup. */
public enum ModuleState {
    ENABLED,
    /** Switched off in config.yml (or by the module itself). */
    DISABLED,
    /** A module it depends on is not running. */
    MISSING_DEPENDENCY,
    /** It threw while starting; the rest of the plugin keeps working. */
    FAILED
}
