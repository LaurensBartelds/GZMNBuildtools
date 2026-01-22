package nl.gzmn.gZMNBuildtools.gradient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages storage and retrieval of saved gradients.
 */
public class GradientStorageManager {

    private final Plugin plugin;
    private final Gson gson;
    private final File storageFile;

    // In-memory cache
    private final Map<UUID, List<SavedGradient>> playerGradients = new ConcurrentHashMap<>();
    private final List<SavedGradient> globalGradients = Collections.synchronizedList(new ArrayList<>());

    public GradientStorageManager(Plugin plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.storageFile = new File(plugin.getDataFolder(), "saved_gradients.json");
    }

    public void load() {
        if (!storageFile.exists()) {
            return;
        }

        try (Reader reader = new FileReader(storageFile)) {
            Type type = new TypeToken<StorageData>() {
            }.getType();
            StorageData data = gson.fromJson(reader, type);

            if (data != null) {
                playerGradients.clear();
                if (data.playerGradients != null) {
                    playerGradients.putAll(data.playerGradients);
                }

                globalGradients.clear();
                if (data.globalGradients != null) {
                    globalGradients.addAll(data.globalGradients);
                }
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load saved gradients", e);
        }
    }

    public void save() {
        try (Writer writer = new FileWriter(storageFile)) {
            StorageData data = new StorageData();
            data.playerGradients = new HashMap<>(playerGradients);
            data.globalGradients = new ArrayList<>(globalGradients);

            gson.toJson(data, writer);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save gradients", e);
        }
    }

    public void saveGradient(SavedGradient gradient) {
        // Add to player list
        playerGradients.computeIfAbsent(gradient.getAuthorInfo(), k -> new ArrayList<>()).add(gradient);

        // Add to global list if public
        if (gradient.isPublic()) {
            globalGradients.add(gradient);
        }

        save();
    }

    public void deleteGradient(UUID playerUuid, String gradientId) {
        List<SavedGradient> userGradients = playerGradients.get(playerUuid);
        if (userGradients != null) {
            userGradients.removeIf(g -> g.getId().equals(gradientId));
        }

        globalGradients.removeIf(g -> g.getId().equals(gradientId) && g.getAuthorInfo().equals(playerUuid));
        save();
    }

    public List<SavedGradient> getPlayerGradients(UUID playerUuid) {
        return playerGradients.getOrDefault(playerUuid, Collections.emptyList());
    }

    public List<SavedGradient> getGlobalGradients() {
        return new ArrayList<>(globalGradients);
    }

    /**
     * Find a saved gradient by name.
     * First checks player's gradients, then global gradients.
     */
    public Optional<SavedGradient> findByName(UUID playerUuid, String name) {
        // First check player's gradients
        List<SavedGradient> playerGrads = getPlayerGradients(playerUuid);
        Optional<SavedGradient> found = playerGrads.stream()
                .filter(g -> g.getName().equalsIgnoreCase(name))
                .findFirst();

        if (found.isPresent()) {
            return found;
        }

        // Then check global gradients
        return globalGradients.stream()
                .filter(g -> g.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    /**
     * Get list of gradient names for a player (for tab completion).
     */
    public List<String> getPlayerGradientNames(UUID playerUuid) {
        List<String> names = new ArrayList<>();

        // Add player's gradients
        getPlayerGradients(playerUuid).forEach(g -> names.add(g.getName()));

        // Add global gradients
        globalGradients.forEach(g -> {
            if (!names.contains(g.getName())) {
                names.add(g.getName());
            }
        });

        return names;
    }

    /**
     * Toggle the public visibility of a gradient.
     */
    public void togglePublic(UUID playerUuid, String gradientId) {
        List<SavedGradient> userGradients = playerGradients.get(playerUuid);
        if (userGradients == null) return;

        for (int i = 0; i < userGradients.size(); i++) {
            SavedGradient g = userGradients.get(i);
            if (g.getId().equals(gradientId)) {
                // Create new SavedGradient with toggled public flag
                SavedGradient updated = new SavedGradient(
                        g.getId(), g.getName(), g.getAuthorInfo(), g.getAuthorName(),
                        g.getPreset(), !g.isPublic(), g.getCreatedAt()
                );
                userGradients.set(i, updated);

                // Update global list
                if (updated.isPublic()) {
                    globalGradients.add(updated);
                } else {
                    globalGradients.removeIf(gg -> gg.getId().equals(gradientId));
                }

                save();
                break;
            }
        }
    }

    // Helper class for JSON serialization
    private static class StorageData {
        Map<UUID, List<SavedGradient>> playerGradients;
        List<SavedGradient> globalGradients;
    }
}
