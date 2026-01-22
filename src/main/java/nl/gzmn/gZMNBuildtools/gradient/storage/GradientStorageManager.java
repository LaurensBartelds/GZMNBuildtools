package nl.gzmn.gZMNBuildtools.gradient.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import nl.gzmn.gZMNBuildtools.gradient.model.SavedGradient;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;


public class GradientStorageManager {

    private final Plugin plugin;
    private final Gson gson;
    private final File storageFile;

    
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
        
        playerGradients.computeIfAbsent(gradient.getAuthorInfo(), k -> new ArrayList<>()).add(gradient);

        
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

    
    public Optional<SavedGradient> findByName(UUID playerUuid, String name) {
        
        List<SavedGradient> playerGrads = getPlayerGradients(playerUuid);
        Optional<SavedGradient> found = playerGrads.stream()
                .filter(g -> g.getName().equalsIgnoreCase(name))
                .findFirst();

        if (found.isPresent()) {
            return found;
        }

        
        return globalGradients.stream()
                .filter(g -> g.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    
    public List<String> getPlayerGradientNames(UUID playerUuid) {
        List<String> names = new ArrayList<>();

        
        getPlayerGradients(playerUuid).forEach(g -> names.add(g.getName()));

        
        globalGradients.forEach(g -> {
            if (!names.contains(g.getName())) {
                names.add(g.getName());
            }
        });

        return names;
    }

    
    public void togglePublic(UUID playerUuid, String gradientId) {
        List<SavedGradient> userGradients = playerGradients.get(playerUuid);
        if (userGradients == null)
            return;

        for (int i = 0; i < userGradients.size(); i++) {
            SavedGradient g = userGradients.get(i);
            if (g.getId().equals(gradientId)) {
                
                SavedGradient updated = new SavedGradient(
                        g.getId(), g.getName(), g.getAuthorInfo(), g.getAuthorName(),
                        g.getPreset(), !g.isPublic(), g.getCreatedAt());
                userGradients.set(i, updated);

                
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

    
    private static class StorageData {
        Map<UUID, List<SavedGradient>> playerGradients;
        List<SavedGradient> globalGradients;
    }
}
