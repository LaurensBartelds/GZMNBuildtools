package nl.gzmn.gZMNBuildtools.core;

import nl.gzmn.gZMNBuildtools.api.BlockFamilyRegistry;
import nl.gzmn.gZMNBuildtools.api.GzmnBuildtoolsApi;
import nl.gzmn.gZMNBuildtools.api.Messages;
import nl.gzmn.gZMNBuildtools.api.PresetRegistry;
import nl.gzmn.gZMNBuildtools.common.AdventureMessages;
import nl.gzmn.gZMNBuildtools.gradient.registry.YamlPresetRegistry;
import nl.gzmn.gZMNBuildtools.shared.preview.PreviewService;
import nl.gzmn.gZMNBuildtools.typereplace.registry.YamlBlockFamilyRegistry;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * The always-on core services every module may use (messages, gradient
 * presets, block families and previews), exposed to other plugins as the
 * public {@link GzmnBuildtoolsApi}.
 */
public final class PluginContext implements GzmnBuildtoolsApi {

    private final Messages messages;
    private final PresetRegistry presetRegistry;
    private final BlockFamilyRegistry blockFamilies;
    private final PreviewService previews;

    public PluginContext(JavaPlugin plugin) {
        this.messages = new AdventureMessages(plugin.getConfig().getBoolean("messages.verbose", false));
        this.presetRegistry = new YamlPresetRegistry(plugin);
        this.blockFamilies = new YamlBlockFamilyRegistry(plugin);
        this.previews = new PreviewService(plugin, messages);
    }

    /** Load all externalized content. */
    public void load() {
        presetRegistry.reload();
        blockFamilies.reload();
    }

    /** Re-reads configuration and content after /gzmnbuildtools reload. */
    public void reload(FileConfiguration config) {
        messages.setVerbose(config.getBoolean("messages.verbose", false));
        load();
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

    public PreviewService previews() {
        return previews;
    }
}
