package nl.gzmn.gZMNBuildtools.core;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Small per-player YAML files ({@code players/<uuid>.yml}), one section per
 * module. Reads are cached; saves happen on the given executor so the main
 * thread never waits for the disk.
 */
public final class PlayerDataStore {

    private final File directory;
    private final Executor saver;
    private final Logger logger;
    private final Map<UUID, YamlConfiguration> loaded = new ConcurrentHashMap<>();
    private final Set<UUID> dirty = new HashSet<>();

    public PlayerDataStore(File directory, Executor saver, Logger logger) {
        this.directory = directory;
        this.saver = saver;
        this.logger = logger;
    }

    private File file(UUID player) {
        return new File(directory, player + ".yml");
    }

    private YamlConfiguration data(UUID player) {
        return loaded.computeIfAbsent(player, id -> {
            YamlConfiguration yaml = new YamlConfiguration();
            File file = file(id);
            if (file.isFile()) {
                try {
                    yaml.load(file);
                } catch (IOException | InvalidConfigurationException e) {
                    logger.log(Level.WARNING, "Could not read player data " + file + "; starting fresh", e);
                }
            }
            return yaml;
        });
    }

    /** Reads a value under {@code module.path}; null when absent. */
    public synchronized Object get(UUID player, String module, String path) {
        return data(player).get(module + "." + path);
    }

    /** The child keys under {@code module.path} (a copy), empty when absent. */
    public synchronized Set<String> keys(UUID player, String module, String path) {
        ConfigurationSection section = data(player).getConfigurationSection(path.isEmpty() ? module : module + "." + path);
        return section == null ? Set.of() : new java.util.TreeSet<>(section.getKeys(false));
    }

    /** Sets a value (null removes it) under {@code module.path} and schedules a save. */
    public synchronized void set(UUID player, String module, String path, Object value) {
        String key = module + "." + path;
        if (value instanceof Map<?, ?> map) {
            // set() would keep the Map as a plain value until the file is reloaded; a section reads back at once.
            data(player).set(key, null);
            data(player).createSection(key, map);
        } else {
            data(player).set(key, value);
        }
        dirty.add(player);
        String snapshot = data(player).saveToString();
        saver.execute(() -> write(player, snapshot));
    }

    private void write(UUID player, String snapshot) {
        try {
            Files.createDirectories(directory.toPath());
            File target = file(player);
            File temp = new File(directory, player + ".yml.tmp");
            Files.writeString(temp.toPath(), snapshot, StandardCharsets.UTF_8);
            Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
            synchronized (this) {
                dirty.remove(player);
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not save player data for " + player, e);
        }
    }

    /** Writes every unsaved file now (on shutdown). */
    public void flush() {
        Map<UUID, String> snapshots = new java.util.HashMap<>();
        synchronized (this) {
            for (UUID player : dirty) {
                snapshots.put(player, data(player).saveToString());
            }
        }
        snapshots.forEach(this::write);
    }
}
