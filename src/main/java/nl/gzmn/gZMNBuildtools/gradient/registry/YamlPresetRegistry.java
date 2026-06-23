package nl.gzmn.gZMNBuildtools.gradient.registry;

import nl.gzmn.gZMNBuildtools.gradient.model.GradientPreset;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

/**
 * {@link nl.gzmn.gZMNBuildtools.api.PresetRegistry} backed by an editable
 * {@code presets.yml} file. The bundled defaults are copied to the data folder
 * on first run; {@link #reload()} re-reads the file.
 */
public class YamlPresetRegistry extends InMemoryPresetRegistry {

    private static final String RESOURCE_NAME = "presets.yml";

    private final Plugin plugin;

    public YamlPresetRegistry(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void reload() {
        presets.clear();

        File file = new File(plugin.getDataFolder(), RESOURCE_NAME);
        if (!file.exists()) {
            try {
                plugin.saveResource(RESOURCE_NAME, false);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().log(Level.WARNING,
                        "Bundled " + RESOURCE_NAME + " is missing; loading defaults from the jar", e);
            }
        }

        YamlConfiguration config = file.exists()
                ? YamlConfiguration.loadConfiguration(file)
                : loadBundled();

        int loaded = parse(config);
        plugin.getLogger().info("Loaded " + loaded + " gradient preset(s) from " + RESOURCE_NAME);
    }

    private YamlConfiguration loadBundled() {
        try (InputStream in = plugin.getResource(RESOURCE_NAME)) {
            if (in == null) {
                return new YamlConfiguration();
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return YamlConfiguration.loadConfiguration(reader);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to read bundled " + RESOURCE_NAME, e);
            return new YamlConfiguration();
        }
    }

    private int parse(YamlConfiguration config) {
        ConfigurationSection root = config.getConfigurationSection("presets");
        if (root == null) {
            return 0;
        }

        int count = 0;
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            try {
                register(readPreset(id, section));
                count++;
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING,
                        "Skipping invalid preset '" + id + "' in " + RESOURCE_NAME + ": " + e.getMessage());
            }
        }
        return count;
    }

    private static GradientPreset readPreset(String id, ConfigurationSection section) {
        List<String> blocks = section.getStringList("blocks");
        GradientPreset.PresetCategory category = parseCategory(section.getString("category"));

        return GradientPreset.builder(id)
                .displayName(section.getString("display-name", id))
                .description(section.getString("description", ""))
                .icon(parseIcon(section.getString("icon"), category))
                .category(category)
                .blocks(blocks)
                .build();
    }

    private static GradientPreset.PresetCategory parseCategory(String raw) {
        if (raw == null) {
            return GradientPreset.PresetCategory.STONE;
        }
        try {
            return GradientPreset.PresetCategory.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return GradientPreset.PresetCategory.STONE;
        }
    }

    private static Material parseIcon(String raw, GradientPreset.PresetCategory category) {
        if (raw != null) {
            Material material = Material.matchMaterial(raw.trim());
            if (material != null) {
                return material;
            }
        }
        return category.getIcon();
    }
}
