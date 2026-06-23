package nl.gzmn.gZMNBuildtools.core;

import nl.gzmn.gZMNBuildtools.api.BlockFamilyRegistry;
import nl.gzmn.gZMNBuildtools.api.GzmnBuildtoolsApi;
import nl.gzmn.gZMNBuildtools.api.Messages;
import nl.gzmn.gZMNBuildtools.api.PresetRegistry;
import nl.gzmn.gZMNBuildtools.common.AdventureMessages;
import nl.gzmn.gZMNBuildtools.gradient.registry.YamlPresetRegistry;
import nl.gzmn.gZMNBuildtools.gradient.service.GradientExecutor;
import nl.gzmn.gZMNBuildtools.gradient.storage.GradientStorageManager;
import nl.gzmn.gZMNBuildtools.typereplace.registry.YamlBlockFamilyRegistry;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Composition root: constructs and owns the plugin's services and exposes them
 * as the public {@link GzmnBuildtoolsApi}. Built once in
 * {@code onEnable}; the same instance is registered with Bukkit's
 * {@link org.bukkit.plugin.ServicesManager}.
 */
public final class PluginContext implements GzmnBuildtoolsApi {

    private final Messages messages;
    private final PresetRegistry presetRegistry;
    private final BlockFamilyRegistry blockFamilies;
    private final GradientExecutor executor;
    private final GradientStorageManager storage;

    public PluginContext(JavaPlugin plugin) {
        this.messages = new AdventureMessages(plugin.getConfig().getBoolean("messages.verbose", false));
        this.presetRegistry = new YamlPresetRegistry(plugin);
        this.blockFamilies = new YamlBlockFamilyRegistry(plugin);
        this.executor = new GradientExecutor();
        this.storage = new GradientStorageManager(plugin);
    }

    /** Load all externalized content and persisted state. */
    public void load() {
        presetRegistry.reload();
        blockFamilies.reload();
        storage.load();
    }

    @Override
    public Messages messages() {
        return messages;
    }

    @Override
    public PresetRegistry presets() {
        return presetRegistry;
    }

    @Override
    public BlockFamilyRegistry blockFamilies() {
        return blockFamilies;
    }

    public GradientExecutor executor() {
        return executor;
    }

    public GradientStorageManager storage() {
        return storage;
    }
}
