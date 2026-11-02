package nl.gzmn.gZMNBuildtools.core.module;

import java.util.List;

/** A module's outcome, for /gzmnbuildtools modules and the startup log. */
public record ModuleStatus(String id, String description, List<String> dependencies, ModuleState state,
                           String reason) {
}
