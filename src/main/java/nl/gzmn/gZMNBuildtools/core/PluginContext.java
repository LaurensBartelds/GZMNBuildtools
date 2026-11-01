package nl.gzmn.gZMNBuildtools.core;

import nl.gzmn.gZMNBuildtools.api.BlockFamilyRegistry;
import nl.gzmn.gZMNBuildtools.api.GzmnBuildtoolsApi;
import nl.gzmn.gZMNBuildtools.api.Messages;
import nl.gzmn.gZMNBuildtools.api.PresetRegistry;
import nl.gzmn.gZMNBuildtools.common.AdventureMessages;
import nl.gzmn.gZMNBuildtools.gradient.registry.YamlPresetRegistry;
import nl.gzmn.gZMNBuildtools.gradient.service.GradientExecutor;
import nl.gzmn.gZMNBuildtools.gradient.storage.GradientStorageManager;
import nl.gzmn.gZMNBuildtools.integration.worldedit.gradient.SmartGradientService;
import nl.gzmn.gZMNBuildtools.preview.PreviewService;
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
    private final JavaPlugin plugin;
    private volatile SmartGradientService smartGradients;
    private volatile PreviewService previews;

    public PluginContext(JavaPlugin plugin) {
        this.plugin = plugin;
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
        if (smartGradients != null) {
            smartGradients.reload(plugin.getConfig());
        }
    }

    /**
     * The smart gradient engine. Created on first use because it reads
     * WorldEdit's block registry, which only exists once WorldEdit/FAWE is enabled.
     */
    public synchronized PreviewService previews() {
        if (previews == null) {
            previews = new PreviewService(plugin, messages);
        }
        return previews;
    }

    public SmartGradientService smartGradients() {
        SmartGradientService service = smartGradients;
        if (service == null) {
            synchronized (this) {
                if (smartGradients == null) {
                    smartGradients = new SmartGradientService(presetRegistry);
                    smartGradients.reload(plugin.getConfig());
                }
                service = smartGradients;
            }
        }
        return service;
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
