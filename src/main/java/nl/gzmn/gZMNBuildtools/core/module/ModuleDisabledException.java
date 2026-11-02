package nl.gzmn.gZMNBuildtools.core.module;

/** Thrown from {@link GzmnModule#enable} when a module decides not to run (not an error). */
public class ModuleDisabledException extends Exception {

    public ModuleDisabledException(String reason) {
        super(reason);
    }
}
