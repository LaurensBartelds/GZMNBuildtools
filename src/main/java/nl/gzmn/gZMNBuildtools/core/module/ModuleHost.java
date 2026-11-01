package nl.gzmn.gZMNBuildtools.core.module;

import nl.gzmn.gZMNBuildtools.core.PluginContext;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

/** The server side of the module system; a fake one is used in tests. */
public interface ModuleHost {

    JavaPlugin plugin();

    PluginContext core();

    Logger logger();

    /** Whether {@code modules.<id>} is switched on. */
    boolean enabledInConfig(String moduleId);

    void registerListener(Listener listener);
}
