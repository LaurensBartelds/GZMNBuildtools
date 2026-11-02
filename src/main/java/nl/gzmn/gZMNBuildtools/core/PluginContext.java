package nl.gzmn.gZMNBuildtools.core;

import nl.gzmn.gZMNBuildtools.api.BlockFamilyRegistry;
import nl.gzmn.gZMNBuildtools.api.GzmnBuildtoolsApi;
import nl.gzmn.gZMNBuildtools.api.Messages;
import nl.gzmn.gZMNBuildtools.api.PresetRegistry;
import nl.gzmn.gZMNBuildtools.common.AdventureMessages;
import nl.gzmn.gZMNBuildtools.common.Translations;
import nl.gzmn.gZMNBuildtools.gradient.registry.YamlPresetRegistry;
import nl.gzmn.gZMNBuildtools.shared.preview.PreviewService;
import nl.gzmn.gZMNBuildtools.shared.worldedit.BlockKnowledge;
import nl.gzmn.gZMNBuildtools.typereplace.registry.YamlBlockFamilyRegistry;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * The always-on core services every module may use (messages, gradient
 * presets, block families, previews, block knowledge, player data and limits), exposed to other plugins as the
 * public {@link GzmnBuildtoolsApi}.
 */
public final class PluginContext implements GzmnBuildtoolsApi {

    private final AdventureMessages messages;
    private final PresetRegistry presetRegistry;
    private final BlockFamilyRegistry blockFamilies;
    private final PreviewService previews;
    private final PlayerDataStore playerData;
    private final Limits limits = new Limits();
    private final JavaPlugin plugin;
    private volatile BlockKnowledge blocks;

    public PluginContext(JavaPlugin plugin) {
        this.messages = new AdventureMessages(plugin.getConfig().getBoolean("messages.verbose", false));
        this.presetRegistry = new YamlPresetRegistry(plugin);
        this.blockFamilies = new YamlBlockFamilyRegistry(plugin);
        this.previews = new PreviewService(plugin, messages);
        this.playerData = new PlayerDataStore(new File(plugin.getDataFolder(), "players"),
                task -> plugin.getServer().getScheduler().runTaskAsynchronously(plugin, task), plugin.getLogger());
        this.plugin = plugin;
    }

    /** Load all externalized content. */
    public void load() {
        messages.setTranslations(Translations.load(plugin, plugin.getConfig().getString("messages.language", "auto")));
        presetRegistry.reload();
        blockFamilies.reload();
        limits.reload(plugin.getConfig()).forEach(p -> plugin.getLogger().warning("config.yml " + p));
    }

    /** Re-reads configuration and content after /gzmnbuildtools reload. */
    public void reload(FileConfiguration config) {
        messages.setVerbose(config.getBoolean("messages.verbose", false));
        load();
        if (blocks != null) {
            blocks.reload(config);
        }
    }

    /**
     * Runtime block knowledge for build-aware tools. Created on first use because
     * it reads WorldEdit's block registry, which exists once WorldEdit/FAWE is enabled.
     */
    public synchronized BlockKnowledge blocks() {
        if (blocks == null) {
            blocks = new BlockKnowledge();
            blocks.reload(plugin.getConfig());
        }
        return blocks;
    }

    /** Per-rank limits (brush radius, blocks per action, ...). */
    public Limits limits() {
        return limits;
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

    /** Per-player saved data (settings, loadouts), one section per module. */
    public PlayerDataStore playerData() {
        return playerData;
    }
}
