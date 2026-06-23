package nl.gzmn.gZMNBuildtools.config;

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
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Registry of gradient presets, loaded from {@code presets.yml}.
 *
 * <p>Presets used to be hardcoded in a static initializer. They now live in an
 * editable YAML file so server owners can add or change them without
 * recompiling; the bundled {@code presets.yml} resource provides the defaults
 * and is copied to the data folder on first run.</p>
 */
public final class GradientPresets {

    private static final String RESOURCE_NAME = "presets.yml";

    private static final Map<String, GradientPreset> PRESETS = new LinkedHashMap<>();

    private GradientPresets() {
    }

    /**
     * (Re)load presets from the data folder's {@code presets.yml}, copying the
     * bundled defaults out first if the file does not yet exist. Safe to call
     * more than once (e.g. for a /reload command).
     */
    public static void load(Plugin plugin) {
        PRESETS.clear();

        File file = new File(plugin.getDataFolder(), RESOURCE_NAME);
        if (!file.exists()) {
            try {
                plugin.saveResource(RESOURCE_NAME, false);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().log(Level.WARNING,
                        "Bundled " + RESOURCE_NAME + " is missing; loading defaults from the jar", e);
            }
        }

        YamlConfiguration config;
        if (file.exists()) {
            config = YamlConfiguration.loadConfiguration(file);
        } else {
            config = loadBundled(plugin);
        }

        int loaded = parse(config, plugin);
        plugin.getLogger().info("Loaded " + loaded + " gradient preset(s) from " + RESOURCE_NAME);
    }

    private static YamlConfiguration loadBundled(Plugin plugin) {
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

    private static int parse(YamlConfiguration config, Plugin plugin) {
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
                GradientPreset preset = readPreset(id, section);
                register(preset);
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

    private static void register(GradientPreset preset) {
        PRESETS.put(preset.getId(), preset);
    }

    public static GradientPreset get(String id) {
        return PRESETS.get(id);
    }

    public static List<GradientPreset> getByCategory(GradientPreset.PresetCategory category) {
        return PRESETS.values().stream()
                .filter(p -> p.getCategory() == category)
                .collect(Collectors.toList());
    }

    public static List<GradientPreset> getAll() {
        return new ArrayList<>(PRESETS.values());
    }

    public static List<String> getAllIds() {
        return new ArrayList<>(PRESETS.keySet());
    }

    public static boolean exists(String id) {
        return PRESETS.containsKey(id);
    }

    public static int count() {
        return PRESETS.size();
    }
}
