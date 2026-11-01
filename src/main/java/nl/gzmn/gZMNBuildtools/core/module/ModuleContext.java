package nl.gzmn.gZMNBuildtools.core.module;

import io.papermc.paper.command.brigadier.Commands;
import nl.gzmn.gZMNBuildtools.core.PluginContext;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * What a module gets to work with: the plugin, the shared core services, and
 * ways to contribute commands, listeners and services. Contributions only take
 * effect if the module enables successfully.
 */
public interface ModuleContext {

    JavaPlugin plugin();

    /** Shared core services: messages, presets, block families, previews. */
    PluginContext core();

    Logger logger();

    /** Registers Brigadier commands for this module. */
    void commands(Consumer<Commands> registrar);

    /** Registers a Bukkit event listener for this module. */
    void listen(Listener listener);

    /** Offers a service to modules that declare this one as a dependency. */
    <T> void provide(Class<T> type, T service);

    /**
     * A service provided by one of this module's declared dependencies.
     *
     * @throws IllegalStateException when no declared dependency provides it
     */
    <T> T require(Class<T> type);
}
