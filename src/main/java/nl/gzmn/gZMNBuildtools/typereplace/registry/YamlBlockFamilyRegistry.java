package nl.gzmn.gZMNBuildtools.typereplace.registry;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

/**
 * {@link nl.gzmn.gZMNBuildtools.api.BlockFamilyRegistry} backed by an editable
 * {@code block-families.yml}. Bundled defaults are copied to the data folder on
 * first run; {@link #reload()} re-reads the file.
 */
public class YamlBlockFamilyRegistry extends InMemoryBlockFamilyRegistry {

    private static final String RESOURCE_NAME = "block-families.yml";

    private final Plugin plugin;

    public YamlBlockFamilyRegistry(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void reload() {
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

        loadFrom(config);
        plugin.getLogger().info("Loaded " + definitions.size() + " block families from " + RESOURCE_NAME);
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
}
